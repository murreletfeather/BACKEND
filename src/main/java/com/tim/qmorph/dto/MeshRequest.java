package com.tim.qmorph.dto;

public class MeshRequest {
    private String meshData; // Can be either JSON format or mesh file format
    private String fileType; // "json" or "mesh"

    public String getMeshData() {
        return meshData;
    }

    public void setMeshData(String meshData) {
        this.meshData = meshData;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}