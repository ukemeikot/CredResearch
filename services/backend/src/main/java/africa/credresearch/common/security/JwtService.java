package africa.credresearch.common.security;

import africa.credresearch.common.config.CredResearchProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Issues and verifies RS256 access tokens (Nimbus JOSE). Minimal claims per Security spec §1. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final CredResearchProperties props;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtService(CredResearchProperties props) {
        this.props = props;
        KeyPairHolder keys = loadOrGenerate(props.auth());
        this.privateKey = keys.privateKey();
        this.publicKey = keys.publicKey();
    }

    public String issueAccessToken(AppUserPrincipal principal) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plus(props.auth().accessTokenTtl());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(principal.userId().toString())
                    .issuer(props.auth().issuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("institution_id", principal.institutionId().toString())
                    .claim("roles", List.copyOf(principal.roles()))
                    .claim("plan", principal.plan())
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    /** Verifies signature + expiry and returns the principal; throws on any invalid token. */
    public AppUserPrincipal parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new RSASSAVerifier(publicKey))) {
                throw new InvalidTokenException("Bad signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!props.auth().issuer().equals(claims.getIssuer())) {
                throw new InvalidTokenException("Bad issuer");
            }
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                throw new InvalidTokenException("Token expired");
            }
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getClaim("roles");
            Set<String> roleSet = roles == null ? Set.of() : roles.stream().collect(Collectors.toUnmodifiableSet());
            return new AppUserPrincipal(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString((String) claims.getClaim("institution_id")),
                    roleSet,
                    (String) claims.getClaim("plan"));
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Malformed token");
        }
    }

    /** Thrown when an access token cannot be trusted. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    private record KeyPairHolder(RSAPrivateKey privateKey, RSAPublicKey publicKey) {}

    private static KeyPairHolder loadOrGenerate(CredResearchProperties.Auth auth) {
        boolean hasKeys = auth.jwtPrivateKey() != null && !auth.jwtPrivateKey().isBlank()
                && auth.jwtPublicKey() != null && !auth.jwtPublicKey().isBlank();
        if (hasKeys) {
            try {
                return new KeyPairHolder(parsePrivate(auth.jwtPrivateKey()), parsePublic(auth.jwtPublicKey()));
            } catch (Exception e) {
                throw new IllegalStateException("Invalid JWT PEM keys", e);
            }
        }
        log.warn("No JWT keys configured — generating an EPHEMERAL RSA keypair. "
                + "Set JWT_PRIVATE_KEY/JWT_PUBLIC_KEY for any non-dev environment.");
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            return new KeyPairHolder((RSAPrivateKey) pair.getPrivate(), (RSAPublicKey) pair.getPublic());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate dev keypair", e);
        }
    }

    private static RSAPrivateKey parsePrivate(String pem) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey parsePublic(String pem) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private static String stripPem(String pem) {
        return pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }
}
