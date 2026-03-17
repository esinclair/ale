package com.ale;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ale.encryption.TenantContext;
import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;

@SpringBootTest
@Tag("integration")
class ALEUserInsertPerformanceTest {

    private static final int  USER_COUNT = 20000;
    private static final int  BATCH_SIZE  = 500;
    private static final UUID TENANT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Barbara", "David", "Elizabeth", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Lisa", "Daniel", "Nancy",
        "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
        "Steven", "Dorothy", "Paul", "Kimberly", "Andrew", "Emily", "Joshua", "Donna",
        "Kevin", "Michelle", "Brian", "Carol", "George", "Amanda", "Timothy", "Melissa",
        "Ronald", "Deborah", "Edward", "Stephanie", "Jason", "Rebecca", "Jeffrey", "Sharon",
        "Ryan", "Laura", "Jacob", "Cynthia", "Gary", "Kathleen", "Nicholas", "Amy",
        "Eric", "Angela", "Jonathan", "Shirley", "Stephen", "Anna", "Larry", "Brenda",
        "Justin", "Pamela", "Scott", "Emma", "Brandon", "Nicole", "Benjamin", "Helen"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
        "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
        "Carter", "Roberts", "Turner", "Phillips", "Evans", "Parker", "Collins", "Edwards",
        "Stewart", "Flores", "Morris", "Nguyen", "Murphy", "Rivera", "Cook", "Rogers",
        "Morgan", "Peterson", "Cooper", "Reed", "Bailey", "Bell", "Gomez", "Kelly",
        "Howard", "Ward", "Cox", "Diaz", "Richardson", "Wood", "Watson", "Brooks"
    };

    private static final String[] DOMAINS = {
        "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com",
        "protonmail.com", "aol.com", "mail.com", "live.com", "msn.com"
    };

    @Autowired
    private ALEUserRepository aleUserRepository;

    @AfterEach
    void cleanup() {
        aleUserRepository.deleteAll();
    }

    @Test
    void insertTwentyThousandALEUsers() {
        long startTime = System.currentTimeMillis();

        int inserted = 0;
        while (inserted < USER_COUNT) {
            int end = Math.min(inserted + BATCH_SIZE, USER_COUNT);
            List<ALEUser> batch = new ArrayList<>(end - inserted);

            for (int i = inserted; i < end; i++) {
                String firstName = FIRST_NAMES[i % FIRST_NAMES.length];
                String lastName  = LAST_NAMES[i  % LAST_NAMES.length];
                String userName  = (firstName + "." + lastName + i).toLowerCase();
                String email     = userName + "@" + DOMAINS[i % DOMAINS.length];
                batch.add(new ALEUser(firstName, lastName, userName, email, TENANT_ID));
            }

            TenantContext.set(TENANT_ID);
            try {
                aleUserRepository.saveAll(batch);
            } finally {
                TenantContext.clear();
            }
            inserted = end;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        long count = aleUserRepository.count();
        assertThat(count).isEqualTo(USER_COUNT);

        System.out.printf("%n=== ALEUser Performance Results ===%n");
        System.out.printf("Inserted : %,d users%n", USER_COUNT);
        System.out.printf("Elapsed  : %,d ms%n", elapsed);
        System.out.printf("Rate     : %.1f inserts/sec%n", USER_COUNT / (elapsed / 1000.0));
    }
}
