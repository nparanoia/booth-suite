package com.booth.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.common.Tokens;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.config.BoothAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 白名单工具的单次执行，带步数上限和错误分类。
 * 读超时在同一次调用里额外重试，最多 {@code timeoutRetries} 次；
 * 权限、商城业务拒绝、相同参数的再次失败都不再打商城。
 */
@Slf4j
@Component
public class ToolExecutor {

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper;
    private final BoothAgentProperties properties;
    private final int maxSteps;
    private final int timeoutRetries;
    private final long toolTimeoutMs;

    public ToolExecutor(ToolRegistry registry, ObjectMapper objectMapper, BoothAgentProperties properties) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.maxSteps = properties.agent().maxToolSteps();
        this.timeoutRetries = properties.agent().timeoutRetries();
        this.toolTimeoutMs = properties.agent().governance().toolTimeout().toMillis();
    }

    public TurnGuard newTurn() {
        return new TurnGuard(maxSteps);
    }

    public StepLog run(TurnGuard guard, String name, Map<String, Object> arguments, ToolContext context) {
        Map<String, Object> args = sanitize(arguments);
        String signature = signature(name, args);
        if (guard.seen(signature)) {
            if (!guard.tryAcquire()) {
                return maxSteps(name, args);
            }
            return StepLog.stop(name, args, ToolErrorKind.SAME_ARGS.name(), 0, false, true, ToolMessages.sameArgs());
        }
        AgentTool tool = registry.find(name);
        if (tool == null) {
            guard.markFailed(signature);
            if (!guard.tryAcquire()) {
                return maxSteps(name, args);
            }
            return StepLog.stop(name, args, ToolErrorKind.NOT_ALLOWED.name(), 0, false, true, ToolMessages.notAllowed());
        }
        String invalid = tool.validate(args);
        if (invalid != null) {
            return invalid(guard, context, name, args, signature, invalid);
        }
        if (!tool.readOnly() && properties.agent().governance().writeCircuitOpen()) {
            if (!guard.tryAcquire()) {
                return maxSteps(name, args);
            }
            return StepLog.stop(name, args, ToolErrorKind.REJECTED.name(), 0, false, true,
                    "写操作暂时关闭（熔断），请稍后再试或致电店主");
        }
        if (context.run().cancelled()) {
            return cancelled(name, args, false);
        }
        context.events().progress(ToolProgress.text(name));
        if ((ToolNames.PREPARE_RETURN.equals(name) || ToolNames.PREPARE_CANCEL.equals(name))
                && context.run().cancelled()) {
            return cancelled(name, args, true);
        }
        int attempts = 0;
        while (true) {
            if (!guard.tryAcquire()) {
                return StepLog.stop(name, args, ToolErrorKind.MAX_STEPS.name(), attempts, attempts > 1, true,
                        ToolMessages.maxSteps(guard.maxSteps()));
            }
            attempts++;
            if (attempts > 1 && context.run().cancelled()) {
                return cancelled(name, args,
                        ToolNames.PREPARE_RETURN.equals(name) || ToolNames.PREPARE_CANCEL.equals(name));
            }
            try {
                ToolResult result = executeWithBudget(tool, context, args);
                guard.recordSuccess();
                audit(context, name, args, true, null, attempts);
                return StepLog.ok(name, args, attempts, result);
            } catch (ToolExecutionException exception) {
                audit(context, name, args, false, exception.kind(), attempts);
                if (exception.kind() == ToolErrorKind.PARAM) {
                    boolean terminal = guard.recordInvalid();
                    guard.markFailed(signature);
                    return StepLog.stop(name, args, ToolErrorKind.PARAM.name(), attempts, attempts > 1, terminal,
                            ToolMessages.param(exception.getMessage(), terminal));
                }
                int retriesUsed = attempts - 1;
                if (exception.kind() == ToolErrorKind.TIMEOUT && retriesUsed < timeoutRetries && guard.remaining() > 0) {
                    continue;
                }
                guard.markFailed(signature);
                String reply = ToolMessages.userFacing(exception, attempts);
                return StepLog.stop(name, args, exception.kind().name(), attempts, attempts > 1, true, reply);
            }
        }
    }

    private ToolResult executeWithBudget(AgentTool tool, ToolContext context, Map<String, Object> args) {
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tool-budget-" + tool.name());
            t.setDaemon(true);
            return t;
        });
        Future<ToolResult> future = pool.submit(() -> tool.execute(context, args));
        try {
            return future.get(toolTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            throw ToolExecutionException.booth(ToolErrorKind.TIMEOUT, 50401, "工具执行超时");
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause() == null ? execution : execution.getCause();
            if (cause instanceof ToolExecutionException toolError) {
                throw toolError;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw ToolExecutionException.booth(ToolErrorKind.TIMEOUT, 50401, "工具执行被中断");
        } finally {
            pool.shutdownNow();
        }
    }

    private static StepLog cancelled(String name, Map<String, Object> args, boolean write) {
        String reply = write ? "已取消，未提交售后。" : "已取消。";
        return StepLog.stop(name, args, "CANCELLED", 0, false, true, reply);
    }

    public StepLog invalidArguments(TurnGuard guard, ToolContext context, String name, String message) {
        return invalid(guard, context, name, Map.of(), signature(name, Map.of()), message);
    }

    private StepLog invalid(TurnGuard guard, ToolContext context, String name, Map<String, Object> args,
                            String signature, String reason) {
        boolean terminal = guard.recordInvalid();
        guard.markFailed(signature);
        if (!guard.tryAcquire()) {
            return maxSteps(name, args);
        }
        audit(context, name, args, false, ToolErrorKind.PARAM, 0);
        return StepLog.stop(name, args, ToolErrorKind.PARAM.name(), 0, false, terminal, ToolMessages.param(reason, terminal));
    }

    private StepLog maxSteps(String name, Map<String, Object> args) {
        return StepLog.stop(name, args, ToolErrorKind.MAX_STEPS.name(), 0, false, true, ToolMessages.maxSteps(maxSteps));
    }

    private void audit(ToolContext context, String name, Map<String, Object> args, boolean success,
                       ToolErrorKind kind, int attempts) {
        String conversationId = context == null ? "-" : context.conversationId();
        String member = context == null ? "-" : Tokens.fingerprint(context.memberToken());
        log.info("tool audit conversationId={} member={} tool={} args={} success={} kind={} attempts={}",
                conversationId, member, name, preview(args), success, kind == null ? "OK" : kind.name(), attempts);
    }

    private Map<String, Object> sanitize(Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (arguments == null) {
            return copy;
        }
        arguments.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String normalized = key.trim();
            if (normalized.equalsIgnoreCase("token")
                    || normalized.equalsIgnoreCase("authorization")
                    || normalized.equalsIgnoreCase("apiKey")) {
                return;
            }
            copy.put(normalized, value);
        });
        return copy;
    }

    private String signature(String name, Map<String, Object> args) {
        AgentTool tool = registry.find(name);
        List<String> keys = tool == null ? List.copyOf(args.keySet()) : tool.signatureKeys();
        Map<String, Object> picked = new TreeMap<>();
        for (String key : keys) {
            if (args.containsKey(key)) {
                picked.put(key, args.get(key));
            }
        }
        try {
            return name + "|" + objectMapper.writeValueAsString(picked);
        } catch (JsonProcessingException exception) {
            return name + "|" + picked;
        }
    }

    private String preview(Map<String, Object> args) {
        try {
            String json = objectMapper.writeValueAsString(args);
            if (json.length() > 180) {
                return json.substring(0, 180);
            }
            return json;
        } catch (JsonProcessingException exception) {
            return args.keySet().toString().toLowerCase(Locale.ROOT);
        }
    }
}
