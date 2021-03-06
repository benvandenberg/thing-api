pipeline {
  agent {
    label "jenkins-maven"
  }
  environment {
    ORG               = 'benvandenberg'
    APP_NAME          = 'thing-api'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
  }
  stages {
    stage('Pre-Build') {
      steps {
        container('maven') {
          script {
            if (env.BRANCH_NAME == 'master') {
              // ensure its not on a detached head
              sh "git checkout $BRANCH_NAME"
              // set the version
              sh "echo \$(jx-release-version) > VERSION"
              sh "mvn versions:set -DnewVersion=\$(cat VERSION)"
            } else if (env.BRANCH_NAME.startsWith('PR-')) {
              // set the version
              sh "mvn versions:set -DnewVersion=0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
            }
          }
        }
      }
    }
    stage('Build') {
      steps {
        container('maven') {
          script {
            if (env.BRANCH_NAME == 'master') {
              sh "mvn deploy"
            } else {
              sh "mvn install"
            }
          }
        }
      }
    }
    stage('Preview') {
      when {
        branch 'PR-*'
      }
      environment {
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
        PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
        HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
      }
      steps {
        container('maven') {
          sh 'export VERSION=$PREVIEW_VERSION && skaffold build -f skaffold.yaml'
          sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$PREVIEW_VERSION"
        }
        dir ('./charts/preview') {
          container('maven') {
            sh "make preview"
            sh "jx preview --app $APP_NAME --dir ../.."
          }
        }
      }
    }
    stage('Release') {
      when {
        branch 'master'
      }
      steps {
        container('maven') {
          sh "git config --global credential.helper store"
          sh "jx step git credentials"
        }
        dir ('./charts/thing-api') {
          container('maven') {
            sh "make tag"
          }
        }
        container('maven') {
          sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
        }
      }
    }
    stage('Promote') {
      when {
        branch 'master'
      }
      steps {
        dir ('./charts/thing-api') {
          container('maven') {
            sh 'jx step changelog --version v\$(cat ../../VERSION)'

            // release the helm chart
            sh 'jx step helm release'

            // promote through all 'Auto' promotion Environments
            sh 'jx promote -b --all-auto --timeout 1h --version \$(cat ../../VERSION)'
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
    failure {
      input """Pipeline failed. 
We will keep the build pod around to help you diagnose any failures. 

Select Proceed or Abort to terminate the build pod"""
    }
  }
}
