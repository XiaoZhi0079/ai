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
                                   @RequestBody Grade grade) {
        return LeeResult.ok(gradeService.create(grade, OperatorResolver.resolve(request)));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
    @GetMapping
    public LeeResult<List<Grade>> list() {
        return LeeResult.ok(gradeService.list());
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER, Role.STUDENT})
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
                                   @RequestBody Grade grade) {
        return gradeService.update(id, grade, OperatorResolver.resolve(request))
                .map(LeeResult::ok)
                .orElseGet(() -> LeeResult.fail("Grade not found"));
    }

    @RoleRequired({Role.ADMIN, Role.TEACHER})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id) {
        boolean deleted = gradeService.delete(id, OperatorResolver.resolve(request));
        return deleted ? LeeResult.ok() : LeeResult.fail("Grade not found");
    }
}
