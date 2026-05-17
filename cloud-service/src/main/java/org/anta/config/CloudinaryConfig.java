package org.anta.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CloudinaryConfig {

    @ConfigProperty(name = "cloudinary.cloud_name")
    String cloudName;

    @ConfigProperty(name = "cloudinary.api_key")
    String apiKey;

    @ConfigProperty(name = "cloudinary.api_secret")
    String apiSecret;

    @Produces
    @ApplicationScoped
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}