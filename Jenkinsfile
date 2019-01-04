pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh './gradlew clean build'
      }
    }
    stage('test') {
      parallel {
        stage('unit test') {
          steps {
            sh './gradlew test'
          }
        }
        stage('coverage test') {
          steps {
            sh './gradlew task'
          }
        }
        stage('lint check') {
          steps {
            sh './gradlew lint'
          }
        }
      }
    }
    stage('assemble') {
      steps {
        sh './gradlew assemble'
      }
    }
    stage('archive artifacts') {
      steps {
        archiveArtifacts '**/*.apk'
      }
    }
  }
}