
-- PostgreSQL database dump
--

\restrict NzPzcWhP0zxwck0JLSClIpTEwA6Dnd6g4Q4GwiiglY9XQpNyqP0w4MQWiTLieHr

-- Dumped from database version 17.8 (9c8634e)
-- Dumped by pg_dump version 17.10

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.flyway_schema_history (
	installed_rank integer NOT NULL,
	version character varying(50),
	description character varying(200) NOT NULL,
	type character varying(20) NOT NULL,
	script character varying(1000) NOT NULL,
	checksum integer,
	installed_by character varying(100) NOT NULL,
	installed_on timestamp without time zone DEFAULT now() NOT NULL,
	execution_time integer NOT NULL,
	success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO neondb_owner;

--
-- Name: idempotency_keys; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.idempotency_keys (
	id uuid NOT NULL,
	auction_id uuid,
	created_at timestamp(6) without time zone NOT NULL,
	key character varying(255) NOT NULL,
	order_id uuid
);


ALTER TABLE public.idempotency_keys OWNER TO neondb_owner;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.orders (
	id uuid NOT NULL,
	auction_id uuid NOT NULL,
	buyer_display_name character varying(100) NOT NULL,
	buyer_id uuid NOT NULL,
	courier character varying(100),
	created_at timestamp(6) without time zone NOT NULL,
	dispute_description text,
	dispute_note text,
	dispute_reason character varying(255),
	dispute_resolution character varying(50),
	disputed_at timestamp(6) without time zone,
	evidence_images text,
	listing_id uuid NOT NULL,
	listing_image_url character varying(500),
	listing_title character varying(255) NOT NULL,
	resolved_at timestamp(6) without time zone,
	seller_display_name character varying(100) NOT NULL,
	seller_id uuid NOT NULL,
	shipped_at timestamp(6) without time zone,
	shipping_city character varying(100),
	shipping_postal_code character varying(10),
	shipping_province character varying(100),
	shipping_street character varying(255),
	status character varying(30) NOT NULL,
	total_amount integer NOT NULL,
	tracking_number character varying(255),
	updated_at timestamp(6) without time zone NOT NULL,
	CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PACKAGED'::character varying, 'SHIPPED'::character varying, 'COMPLETED'::character varying, 'DISPUTED'::character varying, 'RESOLVED'::character varying])::text[])))
);


ALTER TABLE public.orders OWNER TO neondb_owner;

--
-- Name: outbox_events; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.outbox_events (
	id uuid NOT NULL,
	aggregate_id character varying(100),
	aggregate_type character varying(100) NOT NULL,
	attempts integer NOT NULL,
	created_at timestamp(6) without time zone NOT NULL,
	dispatched_at timestamp(6) without time zone,
	event_type character varying(100) NOT NULL,
	last_error text,
	message_key character varying(255),
	payload text NOT NULL,
	status character varying(20) NOT NULL,
	topic character varying(255) NOT NULL,
	updated_at timestamp(6) without time zone NOT NULL,
	CONSTRAINT outbox_events_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.outbox_events OWNER TO neondb_owner;

--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.flyway_schema_history
	ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: idempotency_keys idempotency_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.idempotency_keys
	ADD CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.orders
	ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: outbox_events outbox_events_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.outbox_events
	ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);


--
-- Name: idempotency_keys ukflvbir9xymuvwrfck1hfm3taj; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.idempotency_keys
	ADD CONSTRAINT ukflvbir9xymuvwrfck1hfm3taj UNIQUE (key);


--
-- Name: orders ukg1hlmw1766593e2gfwe8lvu83; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.orders
	ADD CONSTRAINT ukg1hlmw1766593e2gfwe8lvu83 UNIQUE (auction_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


-- PostgreSQL database dump complete
--

\unrestrict NzPzcWhP0zxwck0JLSClIpTEwA6Dnd6g4Q4GwiiglY9XQpNyqP0w4MQWiTLieHr

