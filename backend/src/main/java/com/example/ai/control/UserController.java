package com.example.ai.control;

import com.example.ai.entity.User;
import com.example.ai.pojo.LeeResult;
import com.example.ai.pojo.UserView;
import com.example.ai.service.UserService;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.utils.OperatorResolver;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @RoleRequired({Role.ADMIN})
    @PostMapping
    public LeeResult<UserView> create(HttpServletRequest request,
                                      @RequestBody User user) {
        User saved = userService.create(user, OperatorResolver.resolve(request));
        return LeeResult.ok(toView(saved));
    }

    @RoleRequired({Role.ADMIN})
    @GetMapping
    public LeeResult<List<UserView>> list() {
        List<UserView> views = userService.list().stream().map(this::toView).toList();
        return LeeResult.ok(views);
    }

    @RoleRequired({Role.ADMIN})
    @GetMapping("/{id}")
    public LeeResult<UserView> get(@PathVariable Integer id) {
        return userService.get(id)
                .map(u -> LeeResult.ok(toView(u)))
                .orElseGet(() -> LeeResult.fail("User not found"));
    }

    @RoleRequired({Role.ADMIN})
    @PutMapping("/{id}")
    public LeeResult<UserView> update(HttpServletRequest request,
                                      @PathVariable Integer id,
                                      @RequestBody User user) {
        return userService.update(id, user, OperatorResolver.resolve(request))
                .map(saved -> LeeResult.ok(toView(saved)))
                .orElseGet(() -> LeeResult.fail("User not found"));
    }

    @RoleRequired({Role.ADMIN})
    @DeleteMapping("/{id}")
    public LeeResult<Void> delete(HttpServletRequest request,
                                  @PathVariable Integer id) {
        boolean deleted = userService.delete(id, OperatorResolver.resolve(request));
        return deleted ? LeeResult.ok() : LeeResult.fail("User not found");
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedTime(),
                user.getUpdatedTime()
        );
    }
}
