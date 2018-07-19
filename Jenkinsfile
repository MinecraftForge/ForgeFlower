@Library('forge-shared-library')_

pipeline {
    agent {
        docker {
            image 'gradlewrapper:j10'
            args '-v gradlecache:/gradlecache'
        }
    }
    environment {
        GRADLE_ARGS = '-Dorg.gradle.daemon.idletimeout=5000'
    }

    stages {
        stage('fetch') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: scm.branches,
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[
                        $class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        reference: '',
                        trackingSubmodules: false
                    ], [$class: 'WipeWorkspace']],
                    submoduleCfg: [],
                    userRemoteConfigs: scm.userRemoteConfigs
                ])
            }
        }
        stage('buildandtest') {
            steps {
                sh './gradlew ${GRADLE_ARGS} --refresh-dependencies --continue ForgeFlower:build -x ForgeFlower:test' //Skip tests because we change so much about FF
                script {
                    env.MYVERSION = sh(returnStdout: true, script: './gradlew properties -q | grep "version:" | awk \'{print $2}\'').trim()
                }
            }
            post {
                success {
                    writeChangelog(currentBuild, 'build/changelog.txt')
                    archiveArtifacts artifacts: 'build/changelog.txt', fingerprint: false
                }
            }
        }
        stage('publish') {
            when {
                not {
                    changeRequest()
                }
            }
            environment {
                FORGE_MAVEN = credentials('forge-maven-forge-user')
            }
            steps {
                sh './gradlew ${GRADLE_ARGS} ForgeFlower:publish -PmavenUser=${FORGE_MAVEN_USR} -PmavenPassword=${FORGE_MAVEN_PSW}'
                sh 'curl --user ${FORGE_MAVEN} http://files.minecraftforge.net/maven/manage/promote/latest/net.minecraftforge.forgeflower/${BUILD_NUMBER}'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'ForgeFlower/build/libs/**/*.jar', fingerprint: true
            //junit 'build/test-results/*/*.xml'
            //jacoco sourcePattern: '**/src/*/java'
        }
    }
}