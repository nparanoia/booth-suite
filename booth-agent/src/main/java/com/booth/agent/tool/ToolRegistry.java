package com.booth.agent.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools;

    public ToolRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> map = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            if (map.put(tool.name(), tool) != null) {
                throw new IllegalStateException("重复的工具: " + tool.name());
            }
        }
        this.tools = Map.copyOf(map);
    }

    public AgentTool find(String name) {
        return tools.get(name);
    }

    public List<String> names() {
        return List.copyOf(new TreeSet<>(tools.keySet()));
    }

    /** 按权限过滤可见工具；当前会员场景传 member。 */
    public List<AgentTool> visible(String permission) {
        String need = permission == null ? "member" : permission;
        return names().stream()
                .map(tools::get)
                .filter(tool -> need.equals(tool.requiredPermission()) || "member".equals(tool.requiredPermission()))
                .toList();
    }

    public List<Map<String, Object>> openAiTools() {
        return visible("member").stream().map(tool -> {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description()
                    + (tool.readOnly() ? "" : "（可能写商城）"));
            function.put("parameters", tool.jsonSchema());
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("type", "function");
            wrapper.put("function", function);
            return wrapper;
        }).toList();
    }
}
