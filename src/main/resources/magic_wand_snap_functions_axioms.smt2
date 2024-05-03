;(assert (forall ((s Set<$Snap>)) (!
;  (<= 0 (Set_card s))
;  :pattern ((Set_card s))
;  )))
;(assert (forall ((o $Snap)) (!
;  (not (Set_in o (as Set_empty  Set<$Snap>)))
;  :pattern ((Set_in o (as Set_empty  Set<$Snap>)))
;  )))
;(assert (forall ((s Set<$Snap>)) (!
;  (and
;    (=> (= (Set_card s) 0) (= s (as Set_empty  Set<$Snap>)))
;    (=> (not (= (Set_card s) 0)) (exists ((x $Snap))  (Set_in x s))))
;  :pattern ((Set_card s))
;  )))
;(assert (forall ((r $Snap)) (!
;  (Set_in r (Set_singleton r))
;  :pattern ((Set_singleton r))
;  )))
;(assert (forall ((r $Snap) (o $Snap)) (!
;  (= (Set_in o (Set_singleton r)) (= r o))
;  :pattern ((Set_in o (Set_singleton r)))
;  )))
;(assert (forall ((r $Snap)) (!
;  (= (Set_card (Set_singleton r)) 1)
;  :pattern ((Set_card (Set_singleton r)))
;  )))
;(assert (forall ((a Set<$Snap>) (x $Snap) (o $Snap)) (!
;  (= (Set_in o (Set_unionone a x)) (or (= o x) (Set_in o a)))
;  :pattern ((Set_in o (Set_unionone a x)))
;  )))
;(assert (forall ((a Set<$Snap>) (x $Snap)) (!
;  (Set_in x (Set_unionone a x))
;  :pattern ((Set_unionone a x))
;  )))
;(assert (forall ((a Set<$Snap>) (x $Snap) (y $Snap)) (!
;  (=> (Set_in y a) (Set_in y (Set_unionone a x)))
;  :pattern ((Set_unionone a x) (Set_in y a))
;  )))
;(assert (forall ((a Set<$Snap>) (x $Snap)) (!
;  (=> (Set_in x a) (= (Set_card (Set_unionone a x)) (Set_card a)))
;  :pattern ((Set_card (Set_unionone a x)))
;  )))
;(assert (forall ((a Set<$Snap>) (x $Snap)) (!
;  (=> (not (Set_in x a)) (= (Set_card (Set_unionone a x)) (+ (Set_card a) 1)))
;  :pattern ((Set_card (Set_unionone a x)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (o $Snap)) (!
;  (= (Set_in o (Set_union a b)) (or (Set_in o a) (Set_in o b)))
;  :pattern ((Set_in o (Set_union a b)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (y $Snap)) (!
;  (=> (Set_in y a) (Set_in y (Set_union a b)))
;  :pattern ((Set_union a b) (Set_in y a))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (y $Snap)) (!
;  (=> (Set_in y b) (Set_in y (Set_union a b)))
;  :pattern ((Set_union a b) (Set_in y b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (o $Snap)) (!
;  (= (Set_in o (Set_intersection a b)) (and (Set_in o a) (Set_in o b)))
;  :pattern ((Set_in o (Set_intersection a b)))
;  :pattern ((Set_intersection a b) (Set_in o a))
;  :pattern ((Set_intersection a b) (Set_in o b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (= (Set_union (Set_union a b) b) (Set_union a b))
;  :pattern ((Set_union (Set_union a b) b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (= (Set_union a (Set_union a b)) (Set_union a b))
;  :pattern ((Set_union a (Set_union a b)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (= (Set_intersection (Set_intersection a b) b) (Set_intersection a b))
;  :pattern ((Set_intersection (Set_intersection a b) b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (= (Set_intersection a (Set_intersection a b)) (Set_intersection a b))
;  :pattern ((Set_intersection a (Set_intersection a b)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (=
;    (+ (Set_card (Set_union a b)) (Set_card (Set_intersection a b)))
;    (+ (Set_card a) (Set_card b)))
;  :pattern ((Set_card (Set_union a b)))
;  :pattern ((Set_card (Set_intersection a b)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (o $Snap)) (!
;  (= (Set_in o (Set_difference a b)) (and (Set_in o a) (not (Set_in o b))))
;  :pattern ((Set_in o (Set_difference a b)))
;  :pattern ((Set_difference a b) (Set_in o a))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>) (y $Snap)) (!
;  (=> (Set_in y b) (not (Set_in y (Set_difference a b))))
;  :pattern ((Set_difference a b) (Set_in y b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (and
;    (=
;      (+
;        (+ (Set_card (Set_difference a b)) (Set_card (Set_difference b a)))
;        (Set_card (Set_intersection a b)))
;      (Set_card (Set_union a b)))
;    (=
;      (Set_card (Set_difference a b))
;      (- (Set_card a) (Set_card (Set_intersection a b)))))
;  :pattern ((Set_card (Set_difference a b)))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (=
;    (Set_subset a b)
;    (forall ((o $Snap)) (!
;      (=> (Set_in o a) (Set_in o b))
;      :pattern ((Set_in o a))
;      :pattern ((Set_in o b))
;      )))
;  :pattern ((Set_subset a b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (or
;    (and (Set_equal a b) (= a b))
;    (and
;      (not (Set_equal a b))
;      (and
;        (not (= a b))
;        (and
;          (= (Set_skolem_diff a b) (Set_skolem_diff b a))
;          (not
;            (= (Set_in (Set_skolem_diff a b) a) (Set_in (Set_skolem_diff a b) b)))))))
;  :pattern ((Set_equal a b))
;  )))
;(assert (forall ((a Set<$Snap>) (b Set<$Snap>)) (!
;  (=> (Set_equal a b) (= a b))
;  :pattern ((Set_equal a b))
;  )))

;(assert (forall ((m $MWSF)) (!
;  (<= 0 (Map_card m))
;  :pattern ((Map_card m))
;  )))
;(assert (forall ((m $MWSF)) (!
;  (= (Set_card (Map_domain m)) (Map_card m))
;  :pattern ((Set_card (Map_domain m)))
;  :pattern ((Map_card m))
;  )))
;(assert (forall ((m1 $MWSF) (m2 $MWSF)) (!
;  (=>
;    (Map_disjoint m1 m2)
;    (forall ((k $Snap)) (!
;      (or (not (Set_in k (Map_domain m1))) (not (Set_in k (Map_domain m2))))
;      :pattern ((Set_in k (Map_domain m1)))
;      :pattern ((Set_in k (Map_domain m2)))
;      )))
;  :pattern ((Map_disjoint m1 m2))
;  )))
;(assert (forall ((m1 $MWSF) (m2 $MWSF)) (!
;  (=>
;    (not (Map_disjoint m1 m2))
;    (exists ((k $Snap))
;      (and (Set_in k (Map_domain m1)) (Set_in k (Map_domain m2)))))
;  :pattern ((Map_disjoint m1 m2))
;  )))
;(assert (forall ((k $Snap)) (!
;  (not (Set_in k (Map_domain (as Map_empty  $MWSF))))
;  :pattern ((Set_in k (Map_domain (as Map_empty  $MWSF))))
;  )))
;(assert (forall ((m $MWSF)) (!
;  (and
;    (= (= (Map_card m) 0) (= m (as Map_empty  $MWSF)))
;    (and
;      (=>
;        (not (= (Map_card m) 0))
;        (exists ((u $Snap))
;          (Set_in u (Map_domain m))))
;      (forall ((u $Snap)) (!
;        (=> (Set_in u (Map_domain m)) (not (= (Map_card m) 0)))
;        :pattern ((Set_in u (Map_domain m)))
;        ))))
;  :pattern ((Map_card m))
;  )))
;(assert (forall ((m1 $MWSF) (m2 $MWSF)) (!
;  (=>
;    (and
;      (forall ((k $Snap)) (!
;        (= (Set_in k (Map_domain m1)) (Set_in k (Map_domain m2)))
;        :pattern ((Set_in k (Map_domain m1)))
;        :pattern ((Set_in k (Map_domain m2)))
;        ))
;      (forall ((k $Snap)) (!
;        (=> (Set_in k (Map_domain m1)) (= (Map_apply m1 k) (Map_apply m2 k)))
;        :pattern ((Map_apply m1 k))
;        :pattern ((Map_apply m2 k))
;        )))
;    (Map_equal m1 m2))
;  :pattern ((Map_equal m1 m2))
;  )))
;(assert (forall ((m1 $MWSF) (m2 $MWSF)) (!
;  (=> (Map_equal m1 m2) (= m1 m2))
;  :pattern ((Map_equal m1 m2))
;  )))
;(assert (forall ((m $MWSF) (k1 $Snap) (k2 $Snap) (v $Snap)) (!
;  (and
;    (=>
;      (= k1 k2)
;      (and
;        (Set_in k2 (Map_domain (Map_update m k1 v)))
;        (= (Map_apply (Map_update m k1 v) k2) v)))
;    (=>
;      (not (= k1 k2))
;      (and
;        (=
;          (Set_in k2 (Map_domain (Map_update m k1 v)))
;          (Set_in k2 (Map_domain m)))
;        (=>
;          (Set_in k2 (Map_domain m))
;          (= (Map_apply (Map_update m k1 v) k2) (Map_apply m k2))))))
;  :pattern ((Set_in k2 (Map_domain (Map_update m k1 v))))
;  :pattern ((Set_in k2 (Map_domain m)) (Map_update m k1 v))
;  :pattern ((Map_apply (Map_update m k1 v) k2))
;  )))
;(assert (forall ((m $MWSF) (k $Snap) (v $Snap)) (!
;  (and
;    (=> (Set_in k (Map_domain m)) (= (Map_card (Map_update m k v)) (Map_card m)))
;    (=>
;      (not (Set_in k (Map_domain m)))
;      (= (Map_card (Map_update m k v)) (+ (Map_card m) 1))))
;  :pattern ((Map_card (Map_update m k v)))
;  :pattern ((Map_card m) (Map_update m k v))
;  )))
;(assert (forall ((m $MWSF) (v $Snap)) (!
;  (=>
;    (Set_in v (Map_values m))
;    (and
;      (Set_in (Map_range_domain_skolem m v) (Map_domain m))
;      (= v (Map_apply m (Map_range_domain_skolem m v)))))
;  :pattern ((Set_in v (Map_values m)))
;  )))
;(assert (forall ((m $MWSF) (k $Snap)) (!
;  (=> (Set_in k (Map_domain m)) (> (Set_card (Map_values m)) 0))
;  :pattern ((Map_apply m k))
;  :pattern ((Set_in k (Map_domain m)))
;  )))
;(assert (forall ((m $MWSF) (k $Snap)) (!
;  (=> (Set_in k (Map_domain m)) (Set_in (Map_apply m k) (Map_values m)))
;  :pattern ((Map_apply m k))
;  )))
