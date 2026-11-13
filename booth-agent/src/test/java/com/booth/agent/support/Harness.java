package com.booth.agent.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.booth.agent.audit.ChatTranscriptRepository;
import com.booth.agent.audit.ConfirmAuditRepository;
import com.booth.agent.chat.ChatOrchestrator;
import com.booth.agent.chat.EntryOrderBinder;
import com.booth.agent.chat.RuleChatService;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.confirm.ConfirmService;
import com.booth.agent.confirm.ConfirmStore;
import com.booth.agent.intent.IntentRouter;
import com.booth.agent.knowledge.KnowledgeService;
import com.booth.agent.knowledge.LocalPg;
import com.booth.agent.knowledge.NgramEmbedder;
import com.booth.agent.llm.LlmClient;
import com.booth.agent.llm.LlmToolLoop;
import com.booth.agent.ops.OpsMetrics;
import com.booth.agent.session.SessionService;
import com.booth.agent.trace.TraceService;
import com.booth.agent.tool.AgentTool;
import com.booth.agent.tool.ListReturnsTool;
import com.booth.agent.tool.PrepareCancelTool;
import com.booth.agent.tool.PrepareChangeAddressTool;
import com.booth.agent.tool.PrepareReturnTool;
import com.booth.agent.tool.QueryOrderTool;
import com.booth.agent.tool.SearchPolicyTool;
import com.booth.agent.tool.ToolExecutor;
import com.booth.agent.tool.ToolRegistry;
import com.booth.agent.tool.TransferHumanTool;
import com.booth.agent.write.WriteActionRepository;
import com.booth.agent.write.WriteActionService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

public final class Harness {

    public final FakeStore fakeStore;
    public final ScriptedChatModel model = new ScriptedChatModel();
    public final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    public final ConfirmStore store;
    public final ConfirmService confirm;
    public final SessionService session;
    public final TraceService traces;
    public final OpsMetrics ops;
    public final KnowledgeService knowledge;
    public final ToolRegistry registry;
    public final ToolExecutor executor;
    public final WriteActionRepository writeActions;
    public final ConfirmAuditRepository audits;
    public final ChatTranscriptRepository transcripts;
    public final WriteActionService writes;
    public final ChatOrchestrator orchestrator;
    public final BoothAgentProperties properties;

    public Harness(BoothAgentProperties properties) throws IOException {
        this(properties, new InMemoryConfirmStore(), new FakeStore(), null);
    }

    public Harness(BoothAgentProperties properties, ConfirmStore store, FakeStore fakeStore) throws IOException {
        this(properties, store, fakeStore, null);
    }

    public Harness(BoothAgentProperties properties, ConfirmStore store, FakeStore fakeStore, LlmClient llmClient)
            throws IOException {
        this.properties = properties;
        this.fakeStore = fakeStore;
        this.store = store;
        confirm = new ConfirmService(store, mapper, properties);
        ops = new OpsMetrics(store, mapper);
        traces = new TraceService(store, mapper, ops);
        LocalPg.ensureStarted();
        DataSource dataSource = new DriverManagerDataSource(LocalPg.JDBC_URL, LocalPg.USER, LocalPg.PASSWORD);
        knowledge = new KnowledgeService(properties, dataSource, new NgramEmbedder());
        knowledge.load();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        writeActions = new WriteActionRepository(jdbc);
        writeActions.ensureSchema();
        audits = new ConfirmAuditRepository(jdbc);
        audits.ensureSchema();
        transcripts = new ChatTranscriptRepository(jdbc);
        transcripts.ensureSchema();
        jdbc.update("DELETE FROM write_action_outbox");
        jdbc.update("DELETE FROM confirm_audit");
        jdbc.update("DELETE FROM chat_transcript");
        session = new SessionService(store, mapper, confirm, audits, transcripts, properties);
        writes = new WriteActionService(confirm, writeActions, fakeStore, session, traces, properties);
        List<AgentTool> tools = List.of(
                new QueryOrderTool(fakeStore),
                new ListReturnsTool(fakeStore),
                new PrepareReturnTool(fakeStore, writes),
                new PrepareCancelTool(fakeStore, writes),
                new PrepareChangeAddressTool(fakeStore, properties, writes),
                new TransferHumanTool(properties),
                new SearchPolicyTool(knowledge)
        );
        registry = new ToolRegistry(tools);
        executor = new ToolExecutor(registry, mapper, properties);
        IntentRouter router = new IntentRouter();
        RuleChatService rules = new RuleChatService(router, executor, session, properties);
        LlmClient client = llmClient == null ? model : llmClient;
        LlmToolLoop loop = LlmToolLoop.forTests(client, executor, registry, router, mapper, session, properties);
        EntryOrderBinder entry = new EntryOrderBinder(fakeStore, session);
        orchestrator = new ChatOrchestrator(rules, loop, properties, session, traces, writes, entry);
    }
}
