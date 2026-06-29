from __future__ import annotations

import pika

from persona_agent_service.config.settings import AgentSettings
from persona_agent_service.messaging import constants
from persona_agent_service.messaging.routing import resolve_routing_key
from persona_agent_service.schemas.messages import AgentMessage


class AgentBusPublisher:
    def __init__(self, settings: AgentSettings | None = None):
        self.settings = settings or AgentSettings.from_env()

    def publish(self, message: AgentMessage) -> str:
        routing_key = resolve_routing_key(message.message_type)
        connection = self._connect()
        try:
            channel = connection.channel()
            self.declare_topology(channel)
            channel.basic_publish(
                exchange=constants.AGENT_EXCHANGE,
                routing_key=routing_key,
                body=message.to_json_bytes(),
                properties=pika.BasicProperties(
                    delivery_mode=2,
                    content_type="application/json",
                    message_id=message.message_id,
                    correlation_id=message.correlation_id,
                ),
            )
            return routing_key
        finally:
            connection.close()

    def declare_topology(self, channel) -> None:
        channel.exchange_declare(
            exchange=constants.AGENT_EXCHANGE,
            exchange_type=constants.AGENT_EXCHANGE_TYPE,
            durable=True,
        )
        for queue_name in constants.AGENT_QUEUE_NAMES:
            arguments = None
            if queue_name != constants.DEAD_QUEUE:
                arguments = {
                    "x-dead-letter-exchange": constants.AGENT_EXCHANGE,
                    "x-dead-letter-routing-key": constants.ROUTING_DEAD,
                }
            channel.queue_declare(queue=queue_name, durable=True, arguments=arguments)
            for routing_key in constants.AGENT_QUEUE_BINDINGS[queue_name]:
                channel.queue_bind(
                    queue=queue_name,
                    exchange=constants.AGENT_EXCHANGE,
                    routing_key=routing_key,
                )

    def _connect(self):
        credentials = pika.PlainCredentials(
            self.settings.rabbitmq_username,
            self.settings.rabbitmq_password,
        )
        parameters = pika.ConnectionParameters(
            host=self.settings.rabbitmq_host,
            port=self.settings.rabbitmq_port,
            credentials=credentials,
        )
        return pika.BlockingConnection(parameters)
