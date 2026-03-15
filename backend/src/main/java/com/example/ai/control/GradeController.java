package com.example.ai.control;

import com.example.ai.entity.Grade;
import com.example.ai.pojo.LeeResult;
import com.example.ai.service.GradeService;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.utils.OperatorResolver;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PostMapping
    public LeeResult<Grade> create(HttpServletRequest request,
                                   @RequestBody Grade grade,
                                   @RequestHeader(value = "X-User", required = false) String operator,
                                   @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(gradeService.create(grade, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @GetMapping
    public LeeResult<List<Grade>> list() {
        return LeeResult.ok(gradeService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @GetMapping("/{id}")
    public LeeResult<Grade> get(@PathVariable Integer id) {
        return gradeService.get(id)
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Grade not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @PutMapping("/{id}")
    public LeeResult<Grade> update(HttpServletRequest request,
                                   @PathVariable Integer id,
                                   @RequestBody Grade grade,
                                   @RequestHeader(value = "X-User", required = false) String operator,
                                   @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return gradeService.update(id, grade, OperatorResolver.resolve(request, operator, role))
                    .map(LeeResult::ok)
                    .orElseGet(() -> LeeResult.fail("Grade not found"));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id,
                                  @RequestHeader(value = "X-User", required = false) String operator,
                                  @RequestHeader(value = "X-Role", required = false) String role) {
        boolean deleted = gradeService.delete(id, OperatorResolver.resolve(request, operator, role));
        return deleted ? LeeResult.ok() : LeeResult.fail("Grade not found");
    }
}
