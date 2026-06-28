package com.aitutor.controller;

import com.aitutor.common.ApiResponse;
import com.aitutor.domain.Subject;
import com.aitutor.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectRepository subjectRepository;

    @GetMapping
    public ApiResponse<List<Subject>> getSubjects() {
        return ApiResponse.ok(subjectRepository.findByIsActiveTrueOrderByNameAsc());
    }
}
