pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: gradle
    image: gradle:8.7-jdk21
    command: ["sleep"]
    args: ["infinity"]
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command: ["sleep"]
    args: ["infinity"]
    volumeMounts:
    - name: harbor-token
      mountPath: /kaniko/.docker
  volumes:
  - name: harbor-token
    secret:
      secretName: harbor-secret
      items:
      - key: .dockerconfigjson
        path: config.json
'''
        }
    }

    parameters {
        booleanParam(name: 'FORCE_BUILD_ALL', defaultValue: false, description: '모든 서비스를 강제로 빌드합니다.')
        booleanParam(name: 'FORCE_AUTH', defaultValue: false, description: 'Auth-Service를 강제로 빌드합니다.')
        booleanParam(name: 'FORCE_RIDER', defaultValue: false, description: 'Rider-Service를 강제로 빌드합니다.')
        booleanParam(name: 'FORCE_MAIN', defaultValue: false, description: 'Main-Service를 강제로 빌드합니다.')
        booleanParam(name: 'FORCE_ADMIN', defaultValue: false, description: 'Admin-Service를 강제로 빌드합니다.')
        booleanParam(name: 'FORCE_GATEWAY', defaultValue: false, description: 'Gateway-Service를 강제로 빌드합니다.')
    }

    environment {
        REGISTRY = "harbor.greenart.n-e.kr"
        PROJECT  = "momoolggo"
        SERVICES = "auth-service,rider-service,main-service,admin-service, gateway-service"
    }

    stages {
        stage('Parallel Gradle Build') {
            steps {
                script {
                    def buildTasks = [:]
                    def serviceList = env.SERVICES.split(',')

                    serviceList.each { service ->
                        def serviceName = service.trim()
                        if (shouldBuild(serviceName)) {
                            buildTasks[serviceName] = {
                                container('gradle') {
                                    sh "chmod +x gradlew"
                                    sh "./gradlew :${serviceName}:clean :${serviceName}:build -x test"
                                    echo "--- [${serviceName}] 빌드 결과물 확인 ---"
                                    sh "ls -lh ${serviceName}/build/libs/"
                                }
                            }
                        }
                    }
                    if (buildTasks) {
                        parallel buildTasks
                    } else {
                        echo "빌드할 변경 사항이 없습니다."
                    }
                }
            }
        }

        stage('Sequential Image Push') {
            steps {
                script {
                    def serviceList = env.SERVICES.split(',')
                    serviceList.each { service ->
                        def serviceName = service.trim()
                        if (shouldBuild(serviceName)) {
                            pushImage(serviceName)
                        }
                    }
                }
            }
        }
    }
}

// [수정 포인트] 변경된 파일 경로를 정확히 감지하는 함수
def shouldBuild(String serviceName) {
    def tagMap = [
        'auth-service': 'auth',
        'rider-service': 'rider',
        'main-service': 'main',
        'admin-service': 'admin',
        'gateway-service': 'gateway'
    ]

    // 1. 강제 빌드 파라미터 체크
    def paramName = "FORCE_${tagMap[serviceName].toUpperCase()}"
    if (params.FORCE_BUILD_ALL || params."${paramName}") {
        return true
    }

    // 2. 커밋 메시지 태그 체크
    if (hasCommitTag(tagMap[serviceName])) {
        return true
    }

    // 3. [에러 해결] 변경 이력 기반 체크
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            // affectedFiles를 사용하여 각 파일의 경로에 접근합니다.
            def files = entry.affectedFiles
            for (int k = 0; k < files.size(); k++) {
                def file = files[k]
                if (file.path.contains(serviceName) || file.path.contains("common/")) {
                    return true
                }
            }
        }
    }
    return false
}

def hasCommitTag(String tag) {
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            def msg = entry.msg.toLowerCase()
            if (msg.contains("[build-all]") || msg.contains("[${tag}]")) {
                return true
            }
        }
    }
    return false
}

def pushImage(String serviceName) {
    container('kaniko') {
        echo "Pushing image for ${serviceName}..."
        sh """
        /kaniko/executor --context ${WORKSPACE} \
            --dockerfile ${WORKSPACE}/${serviceName}/Dockerfile \
            --destination ${REGISTRY}/${PROJECT}/${serviceName}:${env.BUILD_NUMBER} \
            --skip-tls-verify
        """
    }
}