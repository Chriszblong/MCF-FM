package COMSETsystem;

public class AgentAction {
    long agentId = -1;
    long resId = -1;
    Type type = Type.NONE;

    private AgentAction() {

    }

    private AgentAction(long agentId, Type type) {
        this.agentId = agentId;
        this.type = type;
    }

    private AgentAction(long agentId, long resId, Type type) {
        this.agentId = agentId;
        this.resId = resId;
        this.type = type;
    }

    public static AgentAction assignTo(long agentId, long resId) {
        return new AgentAction(agentId, resId, Type.ASSIGN);
    }

    public static AgentAction doNothing() {
        return new AgentAction();
    }

    public static AgentAction abort(long agentId) {
        return new AgentAction(agentId, Type.ABORT);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        NONE,
        ASSIGN,
        ABORT
    }
}
