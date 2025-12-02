package contactapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation and validation.
 * Uses HMAC-SHA256 for signing tokens.
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>jwt.secret - Base64-encoded secret key (min 256 bits). <b>Required</b> - application
 *       will fail to start if not configured. Raw strings are still accepted for backward
 *       compatibility but strongly discouraged.</li>
 *   <li>jwt.expiration - Token expiration time in milliseconds (default: 30m)</li>
 * </ul>
 *
 * <p><b>Security Note:</b> The jwt.secret must be configured via environment variable or
 * secrets manager. Never commit secrets to source control.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:1800000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-window:300000}")
    private long refreshWindow;

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the username stored in the token
     */
    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param token the JWT token
     * @param claimsResolver function to extract the desired claim
     * @param <T> the type of the claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(final UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token with extra claims.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(final Map<String, Object> extraClaims, final UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Returns the configured token expiration time.
     *
     * @return expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    /**
     * Validates a JWT token against user details.
     *
     * @param token the JWT token to validate
     * @param userDetails the user details to validate against
     * @return true if the token is valid for the user
     */
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token is expired, so it's not valid
            return false;
        }
    }

    /**
     * Checks if a token is eligible for refresh.
     * A token is eligible if it's still valid OR expired within the refresh window.
     *
     * @param token the JWT token to check
     * @param userDetails the user details to validate against
     * @return true if the token can be refreshed
     */
    public boolean isTokenEligibleForRefresh(final String token, final UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            if (!username.equals(userDetails.getUsername())) {
                return false;
            }
            final Date expiration = extractExpiration(token);
            final long now = System.currentTimeMillis();
            final long expirationTime = expiration.getTime();
            // Token is valid OR expired within the refresh window
            return expirationTime > now || (now - expirationTime) <= refreshWindow;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token is expired - check if within refresh window
            final Claims claims = e.getClaims();
            if (claims == null) {
                return false;
            }
            final String username = claims.getSubject();
            if (!username.equals(userDetails.getUsername())) {
                return false;
            }
            final Date expiration = claims.getExpiration();
            final long now = System.currentTimeMillis();
            final long expirationTime = expiration.getTime();
            return (now - expirationTime) <= refreshWindow;
        }
    }

    /**
     * Returns the configured refresh window.
     *
     * @return refresh window in milliseconds
     */
    public long getRefreshWindow() {
        return refreshWindow;
    }

    private String buildToken(
            final Map<String, Object> extraClaims,
            final UserDetails userDetails,
            final long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException | DecodingException ex) {
            // Fallback for legacy plain-text secrets (documented as discouraged)
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
