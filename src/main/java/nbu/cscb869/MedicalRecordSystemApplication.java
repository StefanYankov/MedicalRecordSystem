package nbu.cscb869;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
public class MedicalRecordSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalRecordSystemApplication.class, args);
    }

}
