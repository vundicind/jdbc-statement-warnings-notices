CREATE OR REPLACE FUNCTION long_running_task(subTaskCount int)
RETURNS boolean AS $$
DECLARE
    i int;
BEGIN
    RAISE NOTICE 'Entering long_running_task(%)...', subTaskCount;
    FOR i IN 1..subTaskCount LOOP
        PERFORM pg_sleep(1);
        RAISE NOTICE 'Done unit of work %', i;
    END LOOP;
    RAISE NOTICE 'Exiting long_running_task(%)', subTaskCount;
    RETURN TRUE;
END;
$$  LANGUAGE plpgsql;