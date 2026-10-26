pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ▼ 폐쇄된 주소 대신 새로운 네이버 공식 저장소 주소로 변경 ▼
        maven { url = uri("https://repository.map.naver.com/archive/maven") }
    }
}
rootProject.name = "GalmaetgilPedometer"
include(":app")
