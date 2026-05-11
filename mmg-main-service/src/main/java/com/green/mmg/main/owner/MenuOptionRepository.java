package com.green.mmg.main.owner;

import com.green.mmg.main.owner.entity.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

    @Query("SELECT mo.optionCategoryNo FROM MenuOption mo " +
            "WHERE mo.optionId = :optionId")
    Long findOptionCategoryNoByOptionId(@Param("optionId") Long optionId);

    List<MenuOption> findByOptionCategoryNo(long optionCategoryNo);
}
