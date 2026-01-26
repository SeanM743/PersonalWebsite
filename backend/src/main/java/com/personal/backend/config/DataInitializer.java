package com.personal.backend.config;

import com.personal.backend.model.FamilyMember;
import com.personal.backend.repository.FamilyMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeFamilyMembers();
    }

    private void initializeFamilyMembers() {
        // Check if family members already exist
        if (familyMemberRepository.count() > 0) {
            return; // Data already initialized
        }

        // Initialize Madelyn (WSU)
        FamilyMember madelyn = new FamilyMember();
        madelyn.setName("Madelyn");
        madelyn.setPrimaryActivity("WSU Student");
        madelyn.setStatus("Doing great at college");
        madelyn.setNotes("Studying hard and making new friends");
        madelyn.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(madelyn);

        // Initialize Evalyn (Driving)
        FamilyMember evalyn = new FamilyMember();
        evalyn.setName("Evalyn");
        evalyn.setPrimaryActivity("Learning to Drive");
        evalyn.setStatus("Getting more confident behind the wheel");
        evalyn.setNotes("Practicing parallel parking and highway driving");
        evalyn.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(evalyn);

        // Initialize Nate (School/Sports)
        FamilyMember nate = new FamilyMember();
        nate.setName("Nate");
        nate.setPrimaryActivity("School & Sports");
        nate.setStatus("Balancing academics and athletics");
        nate.setNotes("Doing well in classes and enjoying team sports");
        nate.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(nate);

        System.out.println("Family Pulse data initialized with 3 family members");
    }
}