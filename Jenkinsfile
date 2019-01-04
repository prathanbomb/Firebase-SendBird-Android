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
        stage('sonarqube') {
          steps {
            sh './gradlew sonarqube'
          }
        }
      }
    }
    stage('assemble') {
      steps {
        sh './gradlew assembleDebug'
      }
    }
  }
}