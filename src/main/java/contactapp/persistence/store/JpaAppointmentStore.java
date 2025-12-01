package contactapp.persistence.store;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import contactapp.persistence.mapper.AppointmentMapper;
import contactapp.persistence.repository.AppointmentRepository;
import contactapp.security.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed {@link AppointmentStore}.
 *
 * <p>Supports per-user data isolation by filtering appointments based on the authenticated user.
 */
@Component
public class JpaAppointmentStore implements AppointmentStore {

    private final AppointmentRepository repository;
    private final AppointmentMapper mapper;

    public JpaAppointmentStore(
            final AppointmentRepository repository,
            final AppointmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Checks if an appointment exists for a specific user.
     *
     * @param id the appointment ID
     * @param user the user who owns the appointment
     * @return true if the appointment exists for the given user
     */
    public boolean existsById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.existsByAppointmentIdAndUser(id, user);
    }

    @Override
    @Deprecated(forRemoval = true)
    public boolean existsById(final String id) {
        throw new UnsupportedOperationException(
                "Use existsById(id, User) to enforce per-user checks");
    }

    /**
     * Saves an appointment for a specific user.
     *
     * @param aggregate the appointment to save
     * @param user the user who owns the appointment
     */
    @Transactional
    public void save(final Appointment aggregate, final User user) {
        if (aggregate == null) {
            throw new IllegalArgumentException("appointment aggregate must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        final Optional<AppointmentEntity> existing = repository.findByAppointmentIdAndUser(
                aggregate.getAppointmentId(), user);
        existing.ifPresentOrElse(
                entity -> {
                    mapper.updateEntity(entity, aggregate);
                    repository.save(entity);
                },
                () -> repository.save(mapper.toEntity(aggregate, user)));
    }

    @Transactional
    public void insert(final Appointment aggregate, final User user) {
        if (aggregate == null) {
            throw new IllegalArgumentException("appointment aggregate must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        repository.save(mapper.toEntity(aggregate, user));
    }

    @Override
    @Deprecated(forRemoval = true)
    public void save(final Appointment aggregate) {
        throw new UnsupportedOperationException(
                "Use save(Appointment, User) instead. This method requires a User parameter.");
    }

    /**
     * Finds an appointment by ID for a specific user.
     *
     * @param id the appointment ID
     * @param user the user who owns the appointment
     * @return optional containing the appointment if found
     */
    public Optional<Appointment> findById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.findByAppointmentIdAndUser(id, user).map(mapper::toDomain);
    }

    @Override
    public Optional<Appointment> findById(final String id) {
        throw new UnsupportedOperationException(
                "Use findById(String, User) to enforce per-user authorization");
    }

    /**
     * Finds all appointments for a specific user.
     *
     * @param user the user who owns the appointments
     * @return list of appointments for the given user
     */
    public List<Appointment> findAll(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.findByUser(user).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * Deletes an appointment by ID for a specific user.
     *
     * @param id the appointment ID
     * @param user the user who owns the appointment
     * @return true if the appointment was deleted, false if not found
     */
    public boolean deleteById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.deleteByAppointmentIdAndUser(id, user) > 0;
    }

    @Override
    @Deprecated(forRemoval = true)
    public boolean deleteById(final String id) {
        throw new UnsupportedOperationException(
                "Use deleteById(String, User) to enforce per-user authorization");
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
