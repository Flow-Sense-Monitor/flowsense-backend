package com.project.flowsense.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.flowsense.entity.LeituraAtual;

public interface LeituraAtualRepository extends JpaRepository<LeituraAtual, String> {
}
