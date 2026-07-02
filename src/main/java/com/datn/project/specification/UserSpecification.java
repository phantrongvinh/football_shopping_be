package com.datn.project.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.datn.project.dto.user.UserFilterDTO;
import com.datn.project.entity.Address;
import com.datn.project.entity.User;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class UserSpecification {
    public static Specification<User> adminFilter(UserFilterDTO filterDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Subquery<Integer> roleSub = query.subquery(Integer.class);
            // Root<User> roleUserRoot = roleSub.from(User.class);
            // Join<User, Role> roleJoin = roleUserRoot.join("roles");
            // roleSub.select(roleUserRoot.get("id"))
            // .where(
            // cb.equal(roleUserRoot.get("id"), root.get("id")),
            // cb.equal(cb.lower(roleJoin.get("name")), "user"));
            // predicates.add(cb.exists(roleSub));

            // Search: email > phone > address
            if (filterDTO.getSearch() != null && !filterDTO.getSearch().trim().isEmpty()) {
                String kw = "%" + filterDTO.getSearch().trim().toLowerCase() + "%";

                Predicate byEmail = cb.like(cb.lower(root.get("email")), kw);
                Predicate byPhone = cb.like(cb.lower(root.get("phone")), kw);

                // Join address
                Subquery<Integer> addressSub = query.subquery(Integer.class);
                Root<Address> addressRoot = addressSub.from(Address.class);
                addressSub.select(addressRoot.get("user").get("id"))
                        .where(
                                cb.equal(addressRoot.get("user"), root),
                                cb.like(cb.lower(addressRoot.get("address")), kw));

                predicates.add(cb.or(byEmail, byPhone, cb.exists(addressSub)));
            }

            // Khoảng birthDay
            if (filterDTO.getBirthDayFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("birthDay"), filterDTO.getBirthDayFrom()));
            }
            if (filterDTO.getBirthDayTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDay"), filterDTO.getBirthDayTo()));
            }

            // Khoảng createdAt
            if (filterDTO.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filterDTO.getCreatedAtFrom()));
            }
            if (filterDTO.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filterDTO.getCreatedAtTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
