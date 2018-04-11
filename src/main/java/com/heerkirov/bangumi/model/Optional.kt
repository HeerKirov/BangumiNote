package com.heerkirov.bangumi.model

import com.heerkirov.bangumi.model.base.Model
import javax.persistence.*

@Entity
@Table(name = "public.optional")
class Optional(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="ID_SEQ")
        @SequenceGenerator(name="ID_SEQ", sequenceName="optional_id_seq", allocationSize = 1)
        @Column(name = "id") var id: Int = 0,
        @Column(name = "allow_register", nullable = false) var allowRegister: Boolean = false
): Model()