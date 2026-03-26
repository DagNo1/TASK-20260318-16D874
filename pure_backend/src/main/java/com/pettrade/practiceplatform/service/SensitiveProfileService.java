package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.profile.SensitiveProfileUpsertRequest;
import com.pettrade.practiceplatform.api.profile.SensitiveProfileView;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.UserSensitiveProfile;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.UserSensitiveProfileRepository;
import com.pettrade.practiceplatform.security.SensitiveDataEncryptor;
import com.pettrade.practiceplatform.security.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveProfileService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveProfileService.class);

    private final UserSensitiveProfileRepository repository;
    private final SensitiveDataEncryptor encryptor;
    private final SensitiveDataMasker masker;

    public SensitiveProfileService(
            UserSensitiveProfileRepository repository,
            SensitiveDataEncryptor encryptor,
            SensitiveDataMasker masker
    ) {
        this.repository = repository;
        this.encryptor = encryptor;
        this.masker = masker;
    }

    @Transactional
    public SensitiveProfileView upsert(User user, SensitiveProfileUpsertRequest request) {
        UserSensitiveProfile profile = repository.findById(user.getId()).orElseGet(UserSensitiveProfile::new);
        profile.setUser(user);
        profile.setPhoneNumberEncrypted(encryptor.encrypt(request.phoneNumber()));
        profile.setIdNumberEncrypted(encryptor.encrypt(request.idNumber()));
        repository.save(profile);

        log.info(
                "Sensitive profile updated for userId={}, phone={}, id={} ",
                user.getId(),
                masker.maskPhone(request.phoneNumber()),
                masker.maskIdNumber(request.idNumber())
        );

        return toView(user.getId(), request.phoneNumber(), request.idNumber());
    }

    @Transactional(readOnly = true)
    public SensitiveProfileView get(User user) {
        UserSensitiveProfile profile = repository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Sensitive profile not found"));

        String phone = encryptor.decrypt(profile.getPhoneNumberEncrypted());
        String id = encryptor.decrypt(profile.getIdNumberEncrypted());
        return toView(user.getId(), phone, id);
    }

    private SensitiveProfileView toView(Long userId, String phone, String id) {
        return new SensitiveProfileView(
                userId,
                masker.maskPhone(phone),
                masker.maskIdNumber(id)
        );
    }
}
