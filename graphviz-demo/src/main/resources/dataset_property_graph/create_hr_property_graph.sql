CREATE OR REPLACE PROPERTY GRAPH myhr
  VERTEX TABLES (
    countries
      KEY ( country_id )
      PROPERTIES ( country_id, country_name, region_id ),
    departments
      KEY ( department_id )
      PROPERTIES ( department_id, department_name, location_id, manager_id ),
    locations
      KEY ( location_id )
      PROPERTIES ( city, country_id, location_id, postal_code, state_province, street_address ),
    jobs
      KEY ( job_id )
      PROPERTIES ( job_id, job_title, max_salary, min_salary ),
    employees
      KEY ( employee_id )
      PROPERTIES ( commission_pct, department_id, email, employee_id, first_name, hire_date, job_id, last_name, manager_id, phone_number, salary ),
    regions
      KEY ( region_id )
      PROPERTIES ( region_id, region_name )
  )
  EDGE TABLES (
    countries AS countries_regions
      SOURCE KEY ( country_id ) REFERENCES countries ( country_id )
      DESTINATION KEY ( region_id ) REFERENCES regions ( region_id )
      NO PROPERTIES,
    departments AS departments_employees
      SOURCE KEY ( department_id ) REFERENCES departments ( department_id )
      DESTINATION KEY ( manager_id ) REFERENCES employees ( employee_id )
      NO PROPERTIES,
    departments AS departments_locations
      SOURCE KEY ( department_id ) REFERENCES departments ( department_id ) 
      DESTINATION KEY ( location_id ) REFERENCES locations ( location_id )
      NO PROPERTIES,
    locations AS locations_countries
      SOURCE KEY ( location_id ) REFERENCES locations ( location_id )
      DESTINATION KEY ( country_id ) REFERENCES countries ( country_id )
      NO PROPERTIES,
    employees AS employees_jobs
      SOURCE KEY ( employee_id ) REFERENCES employees ( employee_id )
      DESTINATION KEY ( job_id ) REFERENCES jobs ( job_id )
      NO PROPERTIES,
    employees AS employees_departments
      SOURCE KEY ( employee_id ) REFERENCES employees ( employee_id )
      DESTINATION KEY ( department_id ) REFERENCES departments ( department_id )
      NO PROPERTIES,
    employees AS employees_employees
      SOURCE KEY ( employee_id ) REFERENCES employees ( employee_id )
      DESTINATION KEY ( manager_id ) REFERENCES employees ( employee_id )
      NO PROPERTIES
  )