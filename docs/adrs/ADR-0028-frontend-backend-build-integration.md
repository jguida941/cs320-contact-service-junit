# ADR-0028: Frontend-Backend Build Integration

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0017](ADR-0017-frontend-stack-and-application-shell.md), [ADR-0019](ADR-0019-deployment-and-packaging.md)

## Context
- The contact app has a Spring Boot backend (Java/Maven) and React frontend (Node/npm).
- For demos, a single deployable artifact is simpler than managing two services.
- Development workflow needs fast feedback: backend changes shouldn't rebuild the UI, and vice versa.
- CI pipeline should produce a single JAR containing both backend and static UI assets.

## Decision
- Use **frontend-maven-plugin** to integrate the npm build into Maven's lifecycle.
- **Phase binding**: Bind all frontend build steps to `prepare-package`, not earlier phases like `compile` or `test`.
- **Workflow separation**:
  - `mvn test` → runs backend tests only (fast feedback)
  - `mvn package` → builds UI + packages everything into single JAR
- **Static asset serving**: Copy built UI (`ui/contact-app/dist`) to `src/main/resources/static` so Spring Boot serves it.

### Maven Plugin Configuration
```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>1.15.1</version>
  <configuration>
    <workingDirectory>ui/contact-app</workingDirectory>
    <nodeVersion>v22.16.0</nodeVersion>
  </configuration>
  <executions>
    <execution>
      <id>install-node-npm</id>
      <phase>prepare-package</phase>
      <goals><goal>install-node-and-npm</goal></goals>
    </execution>
    <execution>
      <id>npm-install</id>
      <phase>prepare-package</phase>
      <goals><goal>npm</goal></goals>
      <configuration><arguments>ci</arguments></configuration>
    </execution>
    <execution>
      <id>npm-build</id>
      <phase>prepare-package</phase>
      <goals><goal>npm</goal></goals>
      <configuration><arguments>run build</arguments></configuration>
    </execution>
  </executions>
</plugin>

<plugin>
  <artifactId>maven-resources-plugin</artifactId>
  <executions>
    <execution>
      <id>copy-frontend</id>
      <phase>prepare-package</phase>
      <goals><goal>copy-resources</goal></goals>
      <configuration>
        <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
        <resources>
          <resource><directory>ui/contact-app/dist</directory></resource>
        </resources>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Development Workflow
```
┌─────────────────┐     ┌─────────────────┐
│   Backend Dev   │     │   Frontend Dev  │
│   (mvn test)    │     │   (npm run dev) │
└────────┬────────┘     └────────┬────────┘
         │                       │
         │    Vite proxy /api    │
         │◄──────────────────────┤
         │    to localhost:8080  │
         │                       │
         ▼                       ▼
┌─────────────────────────────────────────┐
│           mvn package                   │
│  ┌─────────────┐  ┌─────────────┐       │
│  │   npm ci    │  │ npm build   │       │
│  └─────────────┘  └─────────────┘       │
│         │                │              │
│         └────────┬───────┘              │
│                  ▼                      │
│         ┌───────────────┐               │
│         │ Copy to static│               │
│         └───────────────┘               │
│                  ▼                      │
│         ┌───────────────┐               │
│         │  Single JAR   │               │
│         └───────────────┘               │
└─────────────────────────────────────────┘
```

### Vite Development Proxy
```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

## Consequences
- Single `java -jar app.jar` serves both UI and API (same origin, no CORS).
- `mvn test` stays fast for backend iterations; UI only builds on `mvn package`.
- Developers can run `npm run dev` for hot-reload UI development against running backend.
- Node.js is downloaded once per machine by frontend-maven-plugin (cached in `ui/contact-app/node`).
- CI artifacts contain everything needed for deployment.

## Alternatives Considered
| Approach                                  | Pros                | Cons                             | Decision         |
|-------------------------------------------|---------------------|----------------------------------|------------------|
| Separate deployments (UI on GitHub Pages) | Independent scaling | CORS config, two hosts to manage | Rejected for MVP |
| Docker multi-stage build                  | Clean isolation     | More complex CI, requires Docker | Future option    |
| Gradle instead of Maven                   | Modern tooling      | Project already uses Maven       | Rejected         |
| Manual npm build before Maven             | Simple              | Easy to forget, not reproducible | Rejected         |

## Future Considerations
- If the app grows, consider splitting to separate deployments with:
  - UI on CDN/GitHub Pages
  - API behind load balancer
  - CORS configuration in Spring Boot
- Docker packaging can wrap the single JAR for container deployments.

## References
- [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin)
- [Spring Boot Static Content](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.spring-mvc.static-content)
- [Vite Server Proxy](https://vite.dev/config/server-options.html#server-proxy)
