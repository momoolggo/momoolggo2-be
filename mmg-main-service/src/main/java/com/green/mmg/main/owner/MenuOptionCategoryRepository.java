package com.green.mmg.main.owner;

import com.green.mmg.main.owner.entity.MenuOptionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuOptionCategoryRepository extends JpaRepository<MenuOptionCategory, Long> {

    @Query("SELECT moc.menuId FROM MenuOptionCategory moc " +
            "WHERE moc.optionCategoryNo = :optionCategoryNo")
    Long findMenuByOptionCategoryNo(@Param("optionCategoryNo") Long optionCategoryNo);
    List<MenuOptionCategory> findByMenuId(long menuId);
}
