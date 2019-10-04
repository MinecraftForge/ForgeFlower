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
        DISCORD_WEBHOOK = credentials('forge-discord-jenkins-webhook')
        DISCORD_PREFIX = "Job: ForgeFlower Branch: ${BRANCH_NAME} Build: #${BUILD_NUMBER}"
        JENKINS_HEAD = 'https://wiki.jenkins-ci.org/download/attachments/2916393/headshot.png'
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
        stage('notify_start') {
            when {
                not {
                    changeRequest()
                }
            }
            steps {
                discordSend(
                    title: "${DISCORD_PREFIX} Started",
                    successful: true,
                    result: 'ABORTED', //White border
                    thumbnail: JENKINS_HEAD,
                    webhookURL: DISCORD_WEBHOOK
                )
            }
        }
        stage('buildandtest') {
            steps {
                sh './gradlew ${GRADLE_ARGS} --refresh-dependencies --continue ForgeFlower:build -x ForgeFlower:test' //Skip tests because we change so much about FF
                script {
                    env.MYVERSION = sh(returnStdout: true, script: './gradlew properties -q | grep "version:" | awk \'{print $2}\'').trim()
                }
            }
        }
        stage('changelog') {
            when {
                not {
                    changeRequest()
                }
            }
            steps {
                writeChangelog(currentBuild, 'build/changelog.txt')
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
            script {
                archiveArtifacts artifacts: 'ForgeFlower/build/libs/**/*.jar', fingerprint: true
                //junit 'build/test-results/*/*.xml'
                //jacoco sourcePattern: '**/src/*/java'
                if (env.CHANGE_ID == null) { // This is unset for non-PRs
                    discordSend(
                        title: "${DISCORD_PREFIX} Finished ${currentBuild.currentResult}",
                        description: '```\n' + getChanges(currentBuild) + '\n```',
                        successful: currentBuild.resultIsBetterOrEqualTo("SUCCESS"),
                        result: currentBuild.currentResult,
                        thumbnail: JENKINS_HEAD,
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
    }
}