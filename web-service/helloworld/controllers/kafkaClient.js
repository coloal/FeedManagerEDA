const { Kafka } = require('kafkajs')

class KafkaProd {

    constructor(){

        this.kafka_client = new Kafka({
            clientId: 'npm-user-request-notifier',
            // brokers: ['192.168.49.2:32509']
            brokers: ['my-kafka-cluster-kafka-bootstrap.kafka-cluster.svc.cluster.local:9092']
        });

        this.kafka_producer = this.kafka_client.producer();

        this.open_connection();
    }

    async open_connection()
    {
        this.kafka_producer.connect().then( () => { console.log("Kafka producer connected")});
    }

}

module.exports = new KafkaProd();