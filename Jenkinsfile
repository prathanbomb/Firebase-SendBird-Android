pipeline {
  agent any
  stages {
    stage('Git Checkout') {
      steps {
        git(url: 'https://github.com/prathanbomb/Firebase-SendBird-Android.git', branch: 'master', changelog: true, poll: true, credentialsId: '9fb969de-83ac-4c3b-8dcc-f9010a5acfce	')
      }
    }
    stage('Build') {
      steps {
        sh './gradlew clean build'
      }
    }
  }
}