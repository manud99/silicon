;(declare-fun Set_card (Set<$Snap>) Int)
;(declare-const Set_empty Set<$Snap>)
;(declare-fun Set_in ($Snap Set<$Snap>) Bool)
;(declare-fun Set_singleton ($Snap) Set<$Snap>)
;(declare-fun Set_unionone (Set<$Snap> $Snap) Set<$Snap>)
;(declare-fun Set_union (Set<$Snap> Set<$Snap>) Set<$Snap>)
;(declare-fun Set_intersection (Set<$Snap> Set<$Snap>) Set<$Snap>)
;(declare-fun Set_difference (Set<$Snap> Set<$Snap>) Set<$Snap>)
;(declare-fun Set_subset (Set<$Snap> Set<$Snap>) Bool)
;(declare-fun Set_equal (Set<$Snap> Set<$Snap>) Bool)
;(declare-fun Set_skolem_diff (Set<$Snap> Set<$Snap>) $Snap)

(declare-fun MWSF_apply ($MWSF $Snap) $Snap)
;(declare-fun Map_card ($MWSF) Int)
;(declare-fun Map_disjoint ($MWSF $MWSF) Bool)
;(declare-fun Map_domain ($MWSF) Set<$Snap>)
;(declare-const Map_empty $MWSF)
;(declare-fun Map_equal ($MWSF $MWSF) Bool)
;(declare-fun Map_update ($MWSF $Snap $Snap) $MWSF)
;(declare-fun Map_values ($MWSF) Set<$Snap>)
;(declare-fun Map_range_domain_skolem ($MWSF $Snap) $Snap)
