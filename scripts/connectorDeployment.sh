#!/usr/bin/bash 
# Builds .jar with dependencies, builds Connect image, redeploys Connect with new image
eval $(minikube docker-env)

IMAGE_NAME=$(kubectl get deployment.apps/my-connect-cluster-connect -o=jsonpath='{$.spec.template.spec.containers[:1].image}' -n kafka-connect)

if [ -z "$1" ]; then
	echo "Specify maven project."
else
	cd ../$1
	mvn package assembly:single

	OLD_IMAGE_NAME=$IMAGE_NAME
	if [ $IMAGE_NAME = "coloal/custom_strimzi:kafkaconnect-customrssconnector-1" ]; then
		IMAGE_NAME="coloal/custom_strimzi:kafkaconnect-customrssconnector-0"
	else
		IMAGE_NAME="coloal/custom_strimzi:kafkaconnect-customrssconnector-1"
	fi

	echo "------------------------------------------------------------"
	echo "antigua imagen: ${OLD_IMAGE_NAME}"
	echo "nueva imagen: ${IMAGE_NAME}"
	echo "------------------------------------------------------------"
	cd ../KafkaConnectImageBuildingFiles
	docker build . -t $IMAGE_NAME --no-cache
	cd ../resourceDefinitions/kafka-relateds
	sed -i "s?$OLD_IMAGE_NAME?$IMAGE_NAME?" kafkaConnect.yaml
	kubectl apply -f kafkaConnect.yaml -n kafka-connect
fi	