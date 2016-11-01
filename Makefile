run-analysis: run-storage run-kibana run-frontapp

stop-analysis: run-storage run-kibana run-frontapp

run: run-analysis run-collector

stop: stop-analysis stop-collector

run-storage:
	cd behavior-ai-storage && docker-compose up -d
stop-storage:
	cd behavior-ai-storage && docker-compose down -v --remove-orphans

run-kibana:
	cd behavior-ai-kibana && docker-compose up -d
stop-kibana:
	cd behavior-ai-kibana && docker-compose down -v --remove-orphans

run-collector:
	cd behavior-ai-collector && docker-compose up -d
stop-collector:
	cd behavior-ai-collector && docker-compose down -v --remove-orphans

run-frontapp:
	cd behavior-ai-frontapp && docker-compose up -d
stop-frontapp:
	cd behavior-ai-frontapp && docker-compose down -v --remove-orphans