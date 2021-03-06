/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rendezvous.service;

import com.rendezvous.customexception.CompanyIdNotFound;
import com.rendezvous.customexception.IncorrectWorkingHours;
import com.rendezvous.entity.Appointment;
import com.rendezvous.entity.Availability;
import com.rendezvous.entity.Client;
import com.rendezvous.entity.Company;
import com.rendezvous.entity.Role;
import com.rendezvous.model.AvailabilityCalendarProperties;
import com.rendezvous.model.BlockDate;
import com.rendezvous.model.BusinessHoursGroup;
import com.rendezvous.model.CompanyCalendarProperties;
import com.rendezvous.model.CompanyExtendedProps;
import com.rendezvous.model.WorkDayHours;
import com.rendezvous.model.WorkWeek;
import com.rendezvous.repository.AppointmentRepository;
import com.rendezvous.repository.AvailabilityRepository;
import com.rendezvous.repository.ClientRepository;
import com.rendezvous.repository.CompanyRepository;
import com.rendezvous.repository.RoleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private AvailabilityRepository availabilityRepository;

    public Company findCompanyByEmail(String email) {
        Optional<Company> company = companyRepository.findCompanyByUserEmail(email);
        company.orElseThrow(() -> new UsernameNotFoundException("Company " + email + " not found!"));
        return company.get();
    }

    public Company findCompanyById(Integer id) throws CompanyIdNotFound {
        Optional<Company> company = companyRepository.findById(id);
        company.orElseThrow(() -> new CompanyIdNotFound("Company " + id + " not found!"));
        return company.get();
    }

    public void saveCompany(Company company) {
        List<Role> roles = roleRepository.findAll();
        for (Role a : roles) {
            if (a.getRole().equals("ROLE_COMPANY")) {
                company.getUser().setRoleList(Arrays.asList(a));
            }
        }
        String encodedPassword = bCryptPasswordEncoder.encode(company.getUser().getPassword());
        company.getUser().setPassword(encodedPassword);
        companyRepository.save(company);
    }

    //reads a company's avalailable hours, and creates a map. Keys are day1, day2, day3 etc.
    public WorkWeek findWorkingHoursByCompany(Company company) {
        List<Availability> availabilities = availabilityRepository.findAllByCompany(company);

        Map<String, WorkDayHours> week = new HashMap<>();

        for (int i = 1; i <= 7; i++) {
            week.put(String.valueOf(i), null);
        }

        for (Availability av : availabilities) {
            WorkDayHours wdh = new WorkDayHours(av.getOpenTime(), av.getCloseTime());
            week.put(String.valueOf(av.getWeekDay()), wdh);
        }

        WorkWeek workWeek = new WorkWeek(week);

        return workWeek;
    }

    public void saveWorkingHours(Company company, WorkWeek workWeek) throws IncorrectWorkingHours {
        for (Map.Entry<String, WorkDayHours> entry : workWeek.getWeek().entrySet()) {
            System.out.println("Key = " + entry.getKey()
                    + ", Value = " + entry.getValue());

            Integer weekDay = Integer.parseInt(entry.getKey());
            WorkDayHours hours = entry.getValue();

            if (hours.getStartTime() == null && hours.getCloseTime() == null) {
                availabilityRepository.deleteByCompanyAndWeekDay(company, weekDay);
            } else if (hours.getStartTime() == null ^ hours.getCloseTime() == null) {    //XOR operation
                throw new IncorrectWorkingHours("Both Opening Time and Closing Time must be selected");
            } else if (hours.getCloseTime().isBefore(hours.getStartTime())) {
                throw new IncorrectWorkingHours("Closing time can not be before Opening Time");
            } else {
                //save to db
                Availability day;
                Optional<Availability> dayOptional = availabilityRepository.findByCompanyAndWeekDay(company, weekDay);
                if (dayOptional.isPresent()) {
                    day = dayOptional.get();
                    day.setOpenTime(hours.getStartTime());
                    day.setCloseTime(hours.getCloseTime());
                    availabilityRepository.save(day);
                } else {
                    day = new Availability();
                    day.setOpenTime(hours.getStartTime());
                    day.setCloseTime(hours.getCloseTime());
                    day.setCompany(company);
                    day.setWeekDay(weekDay);
                    availabilityRepository.save(day);
                }
            }
        }
    }

    public void updateCompany(Company company) {
        companyRepository.save(company);
    }

    public List<CompanyCalendarProperties> convertPropertiesList(List<Appointment> appointments) {
        List<CompanyCalendarProperties> ccpList = new LinkedList<>();
        LocalDateTime startTime;
        String fullname;
        for (Appointment ap : appointments) {
            Client client = ap.getClient();
            startTime = ap.getDate().atStartOfDay();
            startTime = startTime.plusHours(ap.getTimeslot());
            CompanyExtendedProps cep = new CompanyExtendedProps(client.getTel());
            fullname = client.getFname() + " " + client.getLname();
            CompanyCalendarProperties ccp = new CompanyCalendarProperties(fullname, startTime, startTime.plusHours(1), cep);
            ccpList.add(ccp);
        }
        return ccpList;
    }

    public AvailabilityCalendarProperties getAvailabilityCalendarProperties(Company company, Client client) {
        AvailabilityCalendarProperties availabilityCalendarProperties = new AvailabilityCalendarProperties();

        //finding and adding business hours
        WorkWeek workWeek = findWorkingHoursByCompany(company);

        List<BusinessHoursGroup> businessHours = new ArrayList();

        for (Map.Entry<String, WorkDayHours> entry : workWeek.getWeek().entrySet()) {

            Integer weekDay = Integer.parseInt(entry.getKey());
            WorkDayHours hours = entry.getValue();

            if (hours != null) {
                BusinessHoursGroup businessHoursGroup = new BusinessHoursGroup();

                businessHoursGroup.getDaysOfWeek().add(weekDay);
                businessHoursGroup.setStartTime(hours.getStartTime());
                businessHoursGroup.setEndTime(hours.getCloseTime());

                businessHours.add(businessHoursGroup);
            }
        }
        availabilityCalendarProperties.setBusinessHours(businessHours);

        //finding and adding company events
        List<BlockDate> blockDates = new ArrayList();

        List<Appointment> companyAppointments = appointmentRepository.findByCompany(company);

        for (Appointment ap : companyAppointments) {
            LocalDateTime startTime = ap.getDate().atStartOfDay();
            startTime = startTime.plusHours(ap.getTimeslot());

            LocalDateTime endTime = startTime.plusHours(1);

            blockDates.add(new BlockDate("Date Unavailable", startTime, endTime));
        }

        //finding and adding client events
        List<Appointment> clientAppointments = appointmentRepository.findByClient(client);

        for (Appointment ap : clientAppointments) {
            LocalDateTime startTime = ap.getDate().atStartOfDay();
            startTime = startTime.plusHours(ap.getTimeslot());

            LocalDateTime endTime = startTime.plusHours(1);
            
            String title = ap.getCompany().getDisplayName();

            //testing if the already have an appointment, to make sure the 2 appointments wont show up at the same time
            BlockDate alreadyExistingAppointment = new BlockDate("Date Unavailable", startTime, endTime);
            if (blockDates.contains(alreadyExistingAppointment)) {
                int indexOf = blockDates.indexOf(alreadyExistingAppointment);
                blockDates.set(indexOf, new BlockDate("Appointment with "+title+" already exists", startTime, endTime));
            } else {
                blockDates.add(new BlockDate(title, startTime, endTime));
            }
        }
        
        availabilityCalendarProperties.setBlockDates(blockDates);
        return availabilityCalendarProperties;
    }

}
