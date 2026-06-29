from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.messaging import constants
from persona_agent_service.messaging.publisher import AgentBusPublisher
from persona_agent_service.schemas.enums import AgentMessageType, AgentRole
from persona_agent_service.schemas.messages import AgentMessage


class FakeChannel:
    def __init__(self):
        self.exchange_declarations = []
        self.queue_declarations = []
        self.bindings = []
        self.published = []

    def exchange_declare(self, **kwargs):
        self.exchange_declarations.append(kwargs)

    def queue_declare(self, **kwargs):
        self.queue_declarations.append(kwargs)

    def queue_bind(self, **kwargs):
        self.bindings.append(kwargs)

    def basic_publish(self, **kwargs):
        self.published.append(kwargs)


class FakeConnection:
    def __init__(self, channel):
        self._channel = channel
        self.closed = False

    def channel(self):
        return self._channel

    def close(self):
        self.closed = True


def test_publisher_declares_topology_and_publishes_persistent_message(monkeypatch):
    fake_channel = FakeChannel()
    fake_connection = FakeConnection(fake_channel)

    def fake_blocking_connection(_parameters):
        return fake_connection

    monkeypatch.setattr(
        "persona_agent_service.messaging.publisher.pika.BlockingConnection",
        fake_blocking_connection,
    )

    message = AgentMessage(
        workflowId="workflow-001",
        taskId="task-001",
        sender=AgentRole.PROFILE_MANAGER,
        receiver=AgentRole.BEHAVIOR_AGENT,
        messageType=AgentMessageType.TASK_ASSIGNED,
        payload={"userId": 10001},
    )
    publisher = AgentBusPublisher(AgentSettings())

    routing_key = publisher.publish(message)

    assert routing_key == "agent.task.assigned"
    assert fake_channel.exchange_declarations[0] == {
        "exchange": constants.AGENT_EXCHANGE,
        "exchange_type": constants.AGENT_EXCHANGE_TYPE,
        "durable": True,
    }
    assert {declaration["queue"] for declaration in fake_channel.queue_declarations} == set(constants.AGENT_QUEUE_NAMES)
    assert any(binding["routing_key"] == "agent.task.assigned" for binding in fake_channel.bindings)
    assert fake_channel.published[0]["exchange"] == constants.AGENT_EXCHANGE
    assert fake_channel.published[0]["routing_key"] == "agent.task.assigned"
    assert fake_channel.published[0]["properties"].delivery_mode == 2
    assert fake_channel.published[0]["properties"].content_type == "application/json"
    assert fake_connection.closed is True
