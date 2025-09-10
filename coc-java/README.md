# coc-java

Java library for ClanBoards. Published to GitHub Packages.

- Group: `com.clanboards`
- Artifact: `coc-java`
- Latest release: `0.0.1`
- Snapshots: `0.0.1-SNAPSHOT` and newer
- Java compatibility: compiled for Java 17; tested on Java 17, 21, and 23 in CI

## Add the repository (GitHub Packages)

GitHub Packages requires authentication for both read and publish.

- Environment variables (recommended):
  - `GITHUB_ACTOR=<your_github_username>`
  - `GITHUB_TOKEN=<token with read:packages>`
- Or Gradle user props in `~/.gradle/gradle.properties`:
  - `gpr.user=<your_github_username>`
  - `gpr.key=<token with read:packages>`

### Gradle (Groovy DSL)

```
repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/ChangeinX/coc.py")
    credentials {
      username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
      password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
    }
  }
}

dependencies {
  implementation 'com.clanboards:coc-java:0.0.1'
  // For snapshots:
  // implementation 'com.clanboards:coc-java:0.0.1-SNAPSHOT'
}
```

### Gradle (Kotlin DSL)

```
repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/ChangeinX/coc.py")
    credentials {
      username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
      password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
    }
  }
}

dependencies {
  implementation("com.clanboards:coc-java:0.0.1")
  // Snapshots:
  // implementation("com.clanboards:coc-java:0.0.1-SNAPSHOT")
}
```

### Maven

```
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/ChangeinX/coc.py</url>
  </repository>
  <!-- also keep Maven Central if you rely on transitive deps from Central -->
  <repository>
    <id>central</id>
    <url>https://repo1.maven.org/maven2/</url>
  </repository>
  
</repositories>

<dependencies>
  <dependency>
    <groupId>com.clanboards</groupId>
    <artifactId>coc-java</artifactId>
    <version>0.0.1</version>
  </dependency>
</dependencies>
```

Credentials (settings.xml):

```
<servers>
  <server>
    <id>github</id>
    <username>${env.GITHUB_ACTOR}</username>
    <password>${env.GITHUB_TOKEN}</password>
  </server>
</servers>
```

## Java versions

- The library is compiled targeting Java 17 bytecode.
- CI runs tests on Java 17, 21, and 23 to ensure runtime compatibility.
- You do not need separate artifacts per Java version—the same JAR works on 17+.

## Publishing (for maintainers)

- Releases are published automatically on tags `v*` (e.g., `v0.0.2`).
- Snapshots publish automatically on pushes to `main`/`master` with a `-SNAPSHOT` version derived from the latest tag.

## Troubleshooting

- 401/403 when resolving: ensure your token includes `read:packages` and that the repository URL matches `ChangeinX/coc.py`.
- 409 on publish: versions are immutable on GitHub Packages; bump the version.
