package com.tim.qmorph.controller;

import com.tim.qmorph.dto.MeshRequest;
import com.tim.qmorph.service.MeshProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mesh")
@CrossOrigin(origins = "*") // Enable CORS for Vue.js frontend
public class MeshController {

    private final MeshProcessingService meshProcessingService;

    @Autowired
    public MeshController(MeshProcessingService meshProcessingService) {
        this.meshProcessingService = meshProcessingService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processMesh(@RequestBody MeshRequest request) {
        try {
            String result = meshProcessingService.processMesh(request.getMeshData(), request.getFileType());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing mesh: " + e.getMessage());
        }
    }
}