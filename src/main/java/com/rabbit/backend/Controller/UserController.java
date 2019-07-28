package com.rabbit.backend.Controller;

import com.rabbit.backend.Bean.Credits.CreditsLogListResponse;
import com.rabbit.backend.Bean.User.*;
import com.rabbit.backend.Security.PasswordUtils;
import com.rabbit.backend.Service.CreditsLogService;
import com.rabbit.backend.Service.MailService;
import com.rabbit.backend.Service.PayService;
import com.rabbit.backend.Service.UserService;
import com.rabbit.backend.Utilities.Exceptions.NotFoundException;
import com.rabbit.backend.Utilities.IPUtil;
import com.rabbit.backend.Utilities.Response.FieldErrorResponse;
import com.rabbit.backend.Utilities.Response.GeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.DigestUtils;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private UserService userService;
    private CreditsLogService creditsLogService;
    private PayService payService;
    private MailService mailService;

    @Autowired
    public UserController(UserService userService, CreditsLogService creditsLogService,
                          PayService payService, MailService mailService) {
        this.userService = userService;
        this.creditsLogService = creditsLogService;
        this.payService = payService;
        this.mailService = mailService;
    }

    @GetMapping("/info/{uid}")
    public OtherUser info(@PathVariable("uid") String uid) {
        OtherUser user = userService.selectOtherUserByUid(uid);
        if (user == null) {
            throw new NotFoundException(1, "user doesn't Exist");
        }
        return user;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterForm form, Errors errors) {
        if (errors.hasErrors()) {
            return GeneralResponse.generate(500, FieldErrorResponse.generator(errors));
        }

        String IP = IPUtil.getIPAddress();
        if (!userService.registerLimitCheck(IP)) {
            return GeneralResponse.generate(503, "Request too frequently.");
        }

        Boolean usernameExistenceCheck = userService.exist("username", form.getUsername());
        Boolean emailExistenceCheck = userService.exist("email", form.getEmail());
        if (usernameExistenceCheck) {
            return GeneralResponse.generate(400, "Username already exist.");
        }

        if (emailExistenceCheck) {
            return GeneralResponse.generate(400, "Email already exist.");
        }

        String uid = userService.register(form.getUsername(), form.getPassword(), form.getEmail());
        userService.registerLimitIncrement(IP);
        mailService.sendMail(form.getEmail(), "感谢您注册酷兔网！", "感谢您注册酷兔网！请您在发表帖子时遵守法律法规，文明上网，理性发言！");
        return GeneralResponse.generate(200, uid);
    }

    @PostMapping("/info/password")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> updatePassword(Authentication authentication,
                                              @Valid @RequestBody UpdatePasswordForm form, Errors errors) {
        if (errors.hasErrors()) {
            return GeneralResponse.generate(500, FieldErrorResponse.generator(errors));
        }

        String uid = (String) authentication.getPrincipal();

        User user = userService.selectUser("uid", uid);
        if (user == null) {
            return GeneralResponse.generate(400, "Username or Password invalid.");
        }
        boolean checkResult = DigestUtils.md5DigestAsHex((form.getOldPassword() + user.getSalt()).getBytes()).equals(
                user.getPassword()
        );

        if (!checkResult) {
            return GeneralResponse.generate(400, "Username or Password invalid.");
        }
        userService.updatePassword(uid, form.getNewPassword());
        return GeneralResponse.generate(200);
    }

    @PostMapping("/info/profile")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> updateProfile(Authentication authentication,
                                             @Valid @RequestBody UpdateProfileForm form, Errors errors) {
        if (errors.hasErrors()) {
            return GeneralResponse.generate(500, FieldErrorResponse.generator(errors));
        }
        String uid = (String) authentication.getPrincipal();

        userService.updateProfile(uid, form);
        return GeneralResponse.generate(200);
    }

    @GetMapping("/info/my")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> myProfile(Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        MyUser user = userService.selectMyUser("uid", uid);

        return GeneralResponse.generate(200, user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginForm form, Errors errors) {
        if (errors.hasErrors()) {
            return GeneralResponse.generate(500, FieldErrorResponse.generator(errors));
        }

        String IP = IPUtil.getIPAddress();
        if (!userService.loginLimitCheck(IP)) {
            return GeneralResponse.generate(503, "Request too frequently.");
        }

        User user = userService.selectUser("username", form.getUsername());
        if (user == null) {
            return GeneralResponse.generate(400, "Username or Password invalid.");
        }
        boolean loginResult = PasswordUtils.checkPassword(user.getPassword(), form.getPassword(), user.getSalt());

        if (loginResult) {
            return GeneralResponse.generate(200, userService.loginResponse(user));
        } else {
            userService.loginLimitIncrement(IP);
            return GeneralResponse.generate(400, "Username or Password invalid.");
        }
    }

    @GetMapping("/credits/log/{page}")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> creditsLog(@PathVariable("page") Integer page, Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        CreditsLogListResponse response = new CreditsLogListResponse();

        response.setCount(creditsLogService.count(uid));
        response.setList(creditsLogService.list(uid, page));
        return GeneralResponse.generate(200, response);
    }

    @GetMapping("/purchased/threads/{page}")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> purchasedThread(@PathVariable("page") Integer page, Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        return GeneralResponse.generate(200, payService.threadPurchasedList(uid, page));
    }

    @GetMapping("/purchased/attach/{page}")
    @PreAuthorize("hasAuthority('User')")
    public Map<String, Object> purchasedAttach(@PathVariable("page") Integer page, Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        return GeneralResponse.generate(200, payService.attachPurchasedList(uid, page));
    }
}
