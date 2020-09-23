package UserExamples;

import COMSETsystem.Configuration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This is for getting the time interval index of current timestamp
 */
public class TemporalUtils {
    ZoneId zone;
    ZonedDateTime start;
    ZonedDateTime end;
    int numOfTimeInterval;

    public TemporalUtils(ZoneId zoneId){
        zone = zoneId;
        start = ZonedDateTime.of(GlobalParameters.temporal_start_datetime, zoneId);
        end = ZonedDateTime.of(GlobalParameters.temporal_end_datetime, zoneId);
        numOfTimeInterval = (end.getDayOfYear() - start.getDayOfYear()) * GlobalParameters.numOfTimeIntervalsPerDay;
    }

    public ZonedDateTime getTime(long epochSecond) {
        if (epochSecond != Long.MIN_VALUE) {
            epochSecond = epochSecond / Configuration.timeResolution;
            return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, 0), zone);
        }
        return ZonedDateTime.now();
    }

    public int getIntersectionTemporalIndex(long timestamp){
        ZonedDateTime dateTime = getTime(timestamp);
        int day = dateTime.getDayOfWeek().getValue();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();

        if(!isValid(dateTime)){
            // if out of scope, find the most similar day
            if(dateTime.isBefore(start)){
                int gap = start.getYear() - dateTime.getYear();
                dateTime = dateTime.plusYears(gap);
            }
            if (dateTime.isAfter(end)) {
                int gap = dateTime.getYear() - end.getYear();
                dateTime = dateTime.minusYears(gap);
            }
            // to match the day of week
            int diffDay = day - dateTime.getDayOfWeek().getValue();
            diffDay = Math.abs(diffDay) < 4 ? diffDay : (diffDay > 0 ? diffDay - 7 : diffDay + 7);
            ZonedDateTime tmp = dateTime.plusDays(diffDay);
            if (isValid(tmp)) {
                // within scope
                dateTime = tmp;
            } else {
                // if out of scope, find the most similar day near to either start or end date
                int t = dateTime.getDayOfYear();
                int s = start.getDayOfYear();
                int e = end.getDayOfYear() - 1;

                int diffS = Math.min(365 + s - t, Math.abs(s - t));
                int diffE = Math.min(365 + e - t, Math.abs(e - t));

                if (diffS < diffE) {
                    // near to start
                    int plusDays = (day + 7 - start.getDayOfWeek().getValue()) % 7;
                    dateTime = start.plusDays(plusDays).plusHours(hour).plusMinutes(minute).plusSeconds(second);
                } else {
                    // near to end
                    int minusDays = (end.getDayOfWeek().getValue() + 6 - day) % 7;
                    dateTime = end.minusDays(minusDays + 1).plusHours(hour).plusMinutes(minute).plusSeconds(second);
                }
            }
        }
        return getIntersectionIndex(dateTime);
    }

    /**
     * Call this method to get the index of time interval for current time
     * @param timestamp current time
     * @return the time interval index
     */
    public int findTimeIntervalIndex(long timestamp){
        ZonedDateTime dateTime = getTime(timestamp);
        int day = dateTime.getDayOfWeek().getValue();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();

        if(!isValid(dateTime)){
            // if out of scope, find the most similar day
            if(dateTime.isBefore(start)){
                int gap = start.getYear() - dateTime.getYear();
                dateTime = dateTime.plusYears(gap);
            }
            if (dateTime.isAfter(end)) {
                int gap = dateTime.getYear() - end.getYear();
                dateTime = dateTime.minusYears(gap);
            }
            // to match the day of week
            int diffDay = day - dateTime.getDayOfWeek().getValue();
            diffDay = Math.abs(diffDay) < 4 ? diffDay : (diffDay > 0 ? diffDay - 7 : diffDay + 7);
            ZonedDateTime tmp = dateTime.plusDays(diffDay);
            if (isValid(tmp)) {
                // within scope
                dateTime = tmp;
            } else {
                // if out of scope, find the most similar day near to either start or end date
                int t = dateTime.getDayOfYear();
                int s = start.getDayOfYear();
                int e = end.getDayOfYear() - 1;

                int diffS = Math.min(365 + s - t, Math.abs(s - t));
                int diffE = Math.min(365 + e - t, Math.abs(e - t));

                if (diffS < diffE) {
                    // near to start
                    int plusDays = (day + 7 - start.getDayOfWeek().getValue()) % 7;
                    dateTime = start.plusDays(plusDays).plusHours(hour).plusMinutes(minute).plusSeconds(second);
                } else {
                    // near to end
                    int minusDays = (end.getDayOfWeek().getValue() + 6 - day) % 7;
                    dateTime = end.minusDays(minusDays + 1).plusHours(hour).plusMinutes(minute).plusSeconds(second);
                }
            }
        }
        return getIndex(dateTime);
    }

    private boolean isValid(ZonedDateTime dateTime){
        return (dateTime.isAfter(start) || dateTime.isEqual(start)) && dateTime.isBefore(end);
    }

    private int getIndex(ZonedDateTime dateTime){
        int days = dateTime.getDayOfYear() - start.getDayOfYear();
        int hours = dateTime.getHour() - start.getHour();
        int minutes = dateTime.getMinute() - start.getMinute();
        int seconds = dateTime.getMinute() - start.getSecond();

        int a = days * GlobalParameters.numOfTimeIntervalsPerDay;
        double b = (double) (hours * 3600 + minutes * 60 + seconds)
                / (double) (24 * 60 * 60)
                * (double) GlobalParameters.numOfTimeIntervalsPerDay;
        return a + (int) b;
    }

    private int getIntersectionIndex(ZonedDateTime dateTime){
        int days = dateTime.getDayOfYear() - start.getDayOfYear();
        int hours = dateTime.getHour() - start.getHour();
        int minutes = dateTime.getMinute() - start.getMinute();
        int seconds = dateTime.getMinute() - start.getSecond();

        int a = days * GlobalParameters.numOfIntersectionTimeIntervalPerDay;
        double b = (double) (hours * 3600 + minutes * 60 + seconds)
                / (double) (24 * 60 * 60)
                * (double) GlobalParameters.numOfIntersectionTimeIntervalPerDay;
        return a + (int) b;
    }
}
