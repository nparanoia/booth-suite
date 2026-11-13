package com.booth.agent.tool;

import java.util.List;
import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    Map<String, Object> jsonSchema();

    /** 参与「同一工具 + 同一参数」判重的字段，按名字排序后序列化。 */
    List<String> signatureKeys();

    /** @return null 表示参数合法 */
    String validate(Map<String, Object> args);

    ToolResult execute(ToolContext context, Map<String, Object> args);

    /** 只读工具可对匿名策略放宽；写准备类必须 false。 */
    default boolean readOnly() {
        return true;
    }

    /** 是否只生成确认卡、真正写商城要等用户确认。 */
    default boolean requiresConfirm() {
        return false;
    }

    /** 所需权限码，默认会员登录即可。 */
    default String requiredPermission() {
        return "member";
    }
}
