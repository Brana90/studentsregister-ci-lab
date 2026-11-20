// StudentsRegister: every tool stage runs in a pinned image on the docker agent; the agent needs only Docker (stage 4)
pipeline {
    agent none
    options {
        timestamps()
        skipDefaultCheckout()     // one checkout, in the checkout stage; the tool containers reuse that workspace
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run the test stage')
    }
    environment {
        REGISTRY = '127.0.0.1:5000'
        IMAGE_NAME = 'studentsregister'
        TRIVY = 'aquasec/trivy:0.76.1@sha256:790fd66d1697395d8693d1acaa1ea702406a20f109b6fc7bac8db623eca6c06c'
    }
    stages {
        stage('checkout') {
            agent { label 'docker' }
            steps {
                checkout scm
                script {
                    env.VERSION = readFile('version.txt').trim().replaceFirst('^v', '')
                    env.COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.REVISION = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.TAG = "${env.VERSION}-${env.COMMIT}"
                }
                discoverGitReferenceBuild(referenceJob: 'platform/studentsregister-mb/main')
                echo "Image tag ${env.TAG} from commit ${env.REVISION}"
            }
        }
        stage('build') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320'
                    label 'docker'
                    args '-v maven-repo:/var/maven/.m2 -e HOME=/var/maven -e MAVEN_OPTS=-Duser.home=/var/maven'
                }
            }
            steps {
                sh './mvnw -B -q -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        stage('test') {
            when { expression { params.RUN_TESTS } }
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320'
                    label 'docker'
                    args '-v maven-repo:/var/maven/.m2 -e HOME=/var/maven -e MAVEN_OPTS=-Duser.home=/var/maven'
                }
            }
            steps {
                sh './mvnw -B test'
                junit 'target/surefire-reports/*.xml'
                // gate 1 (tests) is junit + mvnw; gate 2: the lines a change adds must be 70 % covered
                recordCoverage(tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                    qualityGates: [[threshold: 70.0, metric: 'LINE', baseline: 'MODIFIED_LINES', criticality: 'FAILURE']])
                script {
                    if (currentBuild.currentResult == 'FAILURE') {
                        error 'Coverage gate failed: modified lines are less than 70 % covered'
                    }
                }
            }
        }
        stage('sonar') {
            when { branch 'main' }
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320'
                    label 'docker'
                    args '-v maven-repo:/var/maven/.m2 -e HOME=/var/maven -e MAVEN_OPTS=-Duser.home=/var/maven'
                }
            }
            steps {
                withSonarQubeEnv('sonar-lab') {
                    sh './mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar -Dsonar.projectKey=studentsregister -Dsonar.userHome=/var/maven/.m2/sonar'
                }
            }
        }
        stage('quality gate') {
            when { branch 'main' }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('image') {
            agent { label 'docker' }
            steps {
                sh '''
                    docker build \
                      --label org.opencontainers.image.revision=$REVISION \
                      --label org.opencontainers.image.version=$VERSION \
                      -t $REGISTRY/$IMAGE_NAME:$TAG .
                '''
            }
        }
        stage('scan') {
            agent { label 'docker' }
            steps {
                // gate 3: no CRITICAL with a fix in the image's OS packages (Trivy DB from the lab's cache)
                sh '''
                    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock:ro -v trivy-cache:/root/.cache \
                      $TRIVY image --skip-db-update --scanners vuln --pkg-types os --quiet \
                      --severity CRITICAL --ignore-unfixed --exit-code 1 $REGISTRY/$IMAGE_NAME:$TAG
                '''
            }
        }
        stage('push') {
            when { branch 'main' }
            agent { label 'docker' }
            steps {
                sh 'docker push $REGISTRY/$IMAGE_NAME:$TAG'
                sh 'echo $REGISTRY/$IMAGE_NAME:$TAG > /srv/ci-agent/studentsregister.tag'
            }
        }
    }
}
