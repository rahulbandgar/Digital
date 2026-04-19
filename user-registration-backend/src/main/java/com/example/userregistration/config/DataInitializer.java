package com.example.userregistration.config;

import com.example.userregistration.entity.User;
import com.example.userregistration.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(UserRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            repo.save(build("Sophia",   "Martinez",  "sophia.martinez@gmail.com",  "+1-212-555-0101", LocalDate.of(1992, 3, 14), "84 Park Avenue",        "New York",     "USA"));
            repo.save(build("Liam",     "Thompson",  "liam.thompson@outlook.com",  "+44-20-7946-0102", LocalDate.of(1988, 7, 22), "12 Baker Street",       "London",       "UK"));
            repo.save(build("Aisha",    "Khan",      "aisha.khan@yahoo.com",       "+92-300-555-0103", LocalDate.of(1995, 11, 5), "56 Gulberg III",        "Lahore",       "Pakistan"));
            repo.save(build("Noah",     "Williams",  "noah.williams@icloud.com",   "+1-415-555-0104", LocalDate.of(1990, 1, 30), "300 Market Street",     "San Francisco","USA"));
            repo.save(build("Emma",     "Dubois",    "emma.dubois@orange.fr",      "+33-1-555-0105",  LocalDate.of(1993, 6, 18), "24 Rue de Rivoli",      "Paris",        "France"));
            repo.save(build("Hiroshi",  "Tanaka",    "hiroshi.tanaka@docomo.ne.jp","+81-3-5555-0106", LocalDate.of(1987, 9, 3),  "1-2-3 Shibuya",         "Tokyo",        "Japan"));
            repo.save(build("Isabella", "Rossi",     "isabella.rossi@libero.it",   "+39-06-555-0107", LocalDate.of(1996, 4, 27), "Via Nazionale 45",      "Rome",         "Italy"));
            repo.save(build("Carlos",   "Herrera",   "carlos.herrera@gmail.com",   "+52-55-5555-0108",LocalDate.of(1991, 12, 9), "Av. Insurgentes Sur 10","Mexico City",  "Mexico"));
            repo.save(build("Fatima",   "Al-Hassan", "fatima.alhassan@gmail.com",  "+971-50-555-0109",LocalDate.of(1994, 8, 15), "Palm Jumeirah, Villa 7","Dubai",        "UAE"));
            repo.save(build("Oliver",   "Schmidt",   "oliver.schmidt@web.de",      "+49-30-555-0110", LocalDate.of(1989, 2, 20), "Unter den Linden 22",   "Berlin",       "Germany"));
            repo.save(build("Priya",    "Sharma",    "priya.sharma@rediffmail.com","+91-98765-0111",  LocalDate.of(1997, 5, 11), "MG Road, Block B",      "Bangalore",    "India"));
            repo.save(build("Ethan",    "Brown",     "ethan.brown@hotmail.com",    "+1-604-555-0112", LocalDate.of(1985, 10, 7), "999 Robson Street",     "Vancouver",    "Canada"));
        };
    }

    private User build(String first, String last, String email, String phone,
                       LocalDate dob, String address, String city, String country) {
        User u = new User();
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPhone(phone);
        u.setDateOfBirth(dob);
        u.setAddress(address);
        u.setCity(city);
        u.setCountry(country);
        return u;
    }
}
