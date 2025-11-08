package nbu.cscb869.web.viewmodels;

import java.time.LocalTime;

/**
 * A view model representing a single time slot for appointment scheduling.
 * It holds the time and its availability status.
 */
public class TimeSlot {
    private final LocalTime time;
    private final boolean isAvailable;

    public TimeSlot(final LocalTime time, final boolean isAvailable) {
        this.time = time;
        this.isAvailable = isAvailable;
    }

    public LocalTime getTime() {
        return time;
    }

    public boolean isAvailable() {
        return isAvailable;
    }
}
