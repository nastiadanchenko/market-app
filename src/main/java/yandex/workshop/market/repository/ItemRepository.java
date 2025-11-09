package yandex.workshop.market.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import yandex.workshop.market.entity.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Modifying
    @Query(value = "update items set count = count + 1 WHERE id = :id")
    void increaseItemCount(Long id);

    @Modifying
    @Query(value = "update items set count = count - 1 WHERE id = :id AND count > 0")
    void reduceItemCount(Long id);


    @Query("SELECT i FROM items i WHERE i.count > :i")
    List<Item> findByCountGreaterThan(int i);

    @Modifying
    @Query("update items set count = 0 WHERE id = :itemId")
    void resetItemCount(Long itemId);
}
