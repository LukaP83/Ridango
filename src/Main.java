import java.io.File;
import java.io.FileNotFoundException;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.*;
import java.time.LocalTime;

public class Main {
    public static void main(String[] args) {
        List<String> trips = busTrips("2",3,"Absolute");

        for (String entry : trips)
            System.out.println(entry);

    }

    public static List<String> busTrips(String stop_id,int num_buses,String abs_rel){

        LocalTime time = LocalTime.now();
//        LocalTime time = LocalTime.parse("12:00"); //If we want to set our own time

        Map<String, String[]> route_direction = get_route_direction();
        List<String[]> direction_route_time = get_direction_route_time(stop_id, route_direction);
        route_direction = null;
        Map<String, List<LocalTime>> bus_arrivals = all_arrivals(direction_route_time);
        direction_route_time = null;
        List<String> req_arrivals = get_req_arrivals(num_buses, abs_rel, time, bus_arrivals);

        return req_arrivals;
    }

    public static List<String[]> get_direction_route_time(String stop_id, Map<String, String[]> route_direction){
    /*From the stop_times.txt (which is in .csv format) we are extracting all the trips which make a stop which has
    appropriate stop_id. At the same time we are matching trip_id with its route_id and direction_id which can
    be found in the route_direction map. We save this information in the list of arrays of strings, where array of
    strings is of the following format {route_id, direction_id, arrival_time}.*/
        List<String[]> trip_times = new ArrayList<String[]>();
        try {
            File stop_times = new File("./gtfs/stop_times.txt");
            Scanner myReader = new Scanner(stop_times);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split(",",0);

                if(stop_id.equals(data[3])){
                    String route_id = route_direction.get(data[0])[0];
                    String direction_id = route_direction.get(data[0])[1];
                    String arrival_time = data[1];

                    String[] temp = {route_id,direction_id,arrival_time};
                    trip_times.add(temp);
                }

            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not read stop_times.txt!");
        }

        return trip_times;
    }

    public static Map<String, String[]> get_route_direction(){
        /*We are loading data from the trips.txt file which is in .csv format
        and creating a map where key = trip_id and value is a list which contains
        route_id and direction_id*/
        Map<String, String[]> route_direction = new HashMap<String, String[]>();
        try {
            File trips = new File("./gtfs/trips.txt");
            Scanner myReader = new Scanner(trips);
            while (myReader.hasNextLine()) {
                String[] data = myReader.nextLine().split(",",0);
                String route_id = data[0];
                String direction_id = data[5];
                String trip_id = data[2];

                String[] temp = {route_id,direction_id};
                route_direction.put(trip_id,temp);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not read trips.txt!");
        }
        return route_direction;
    }

    public static Map<String, List<LocalTime>> all_arrivals(List<String[]> direction_route_time){
        /*Creates a map where the keys are route_id for the direct routes and direct_id+'R' for the
        reverse routes, and values of the map are the arrivals that are saved into the list
        in LocalTime format*/
        Map<String, List<LocalTime>> bus_arrivals = new HashMap<String, List<LocalTime>>();

        for(String[] current: direction_route_time){
            //Checking if the route is a Go or Return route and adding R to the returning ones
            String route_id = current[0] + ((current[1].equals("0"))?"":"R");

            //Checking if the route is already present in the Map, if not we initialize it
            if(!bus_arrivals.containsKey(route_id)){
                bus_arrivals.put(route_id,new ArrayList<LocalTime>());
            }

            //Appending the time to the list of arrivals we have already found
            bus_arrivals.get(route_id).add(LocalTime.parse(current[2]));
        }

        //Sorting arrival times for each route
        for (Map.Entry<String, List<LocalTime>> entry : bus_arrivals.entrySet())
            Collections.sort(entry.getValue());

        return bus_arrivals;
    }

    public static List<String> get_req_arrivals(int num_buses, String abs_rel,LocalTime curr_time,Map<String, List<LocalTime>> bus_arrivals){
        /*From Map bus_arrivals (which is of the format Key = route_id and Values = sorted times of arrival) we make a
        list of strings in which we store strings in the following format "/route_id/: /1st_arrival/ /2nd_arrival/ ...", where:
            1) If we requested Absolute time arrivals will be presented as hh:mm
            2) If we requested Relative time arrivals will be presented as mm+'min'*/
        List<String> req_arrivals = new ArrayList<String>();

        for(Map.Entry<String, List<LocalTime>> entry : bus_arrivals.entrySet()){
            /*We are taking every route and its time arrivals in order to check if there are
            any more arrivals for today*/
            String route_id = entry.getKey();
            List<LocalTime> arrivals = entry.getValue();

            String route_arrivals = String.format("%5s:",route_id);
            String temp = "";
            int buses_found = 0;

            for(int i = 0; i < arrivals.size() && buses_found < num_buses;i++){
                /*We are going over all of the arrivals until we come to the end or we find
                as many arrivals as requested by main function.*/
                LocalTime arrival = arrivals.get(i);
                if(arrival.isAfter(curr_time)){
                    route_arrivals += format_time(curr_time, arrival, abs_rel);
                    buses_found++;
                }
            }
            //If there are no arrivals for the route we are not going to add it
            if(buses_found > 0)
                req_arrivals.add(route_arrivals);
        }
        return req_arrivals;
    }

    public static String format_time(LocalTime curr_time, LocalTime arrival,String abs_rel){

        String time;
        if(abs_rel.equals("Absolute")) {
            int minutes = arrival.getMinute();
            int hours = arrival.getHour();
            time = String.format(" %02d:%02d", hours, minutes);
        }
        else{
            long minutes = curr_time.until(arrival,ChronoUnit.MINUTES);
            time = String.format(" %3dmin",minutes);
        }
        return time;
    }
}