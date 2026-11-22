package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Integer> {

    List<Faq> findAllByOrderByIdFaqAsc();

    List<Faq> findByActivaTrueOrderByIdFaqAsc();
}