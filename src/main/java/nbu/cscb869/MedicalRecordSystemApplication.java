package nbu.cscb869;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MedicalRecordSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalRecordSystemApplication.class, args);
    }

}
